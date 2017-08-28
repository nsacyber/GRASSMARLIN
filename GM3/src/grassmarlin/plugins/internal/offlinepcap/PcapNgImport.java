package grassmarlin.plugins.internal.offlinepcap;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PcapNgImport {
    private final ImportItem source;
    private final BlockingQueue<Object> packetQueue;
    private final RuntimeConfiguration config;
    protected final HashMap<Integer, IPlugin.IPacketHandler> handlerForInterface;
    private boolean done;
    private int idxFrame;

    protected boolean isLittleEndian = false;
    protected Map<Integer, Long> timestampResolutions = new HashMap<>();
    protected int idNextBlock = 0;

    private final ByteBuffer bufEnhancedHeader = ByteBuffer.allocateDirect(20);

    public PcapNgImport(final RuntimeConfiguration config, final ImportItem source) throws IllegalStateException {
        this.source = source;
        this.packetQueue = new ArrayBlockingQueue<>(100);
        this.handlerForInterface = new HashMap<>();
        this.config = config;
        this.done = false;
        this.idxFrame = 1;

        if(!validateFileFormat(this.source.getPath())) {
            throw new IllegalArgumentException("File failed validation.");
        }

        parseSource();
    }

    public static boolean validateFileFormat(final Path path) {
        try(SeekableByteChannel reader = Files.newByteChannel(path)) {
            ByteBuffer buffer = ByteBuffer.allocate(12);
            reader.read(buffer);
            int bom = buffer.getInt(8);
            if(bom == 0x1A2B3C4D || bom == 0x4D3C2B1A) {
                return true;
            }

            //Check for pcap
            bom = buffer.getInt(0);
            if(bom == 0xA1B2C3D4 || bom == 0xD4C3B2A1) {
                Logger.log(Logger.Severity.WARNING, "The file appears to be a Pcap file, not a PcapNg file (%s)", path.getFileName());
            }
            return false;
        } catch(IOException ex) {
            return false;
        }
    }

    protected static long divisorFromTsresol(byte resolution) throws IOException {
        if((resolution & 0x80) == 0x80) {
            //MSB is 1 -> power of 2
            if((resolution & 0x7F) > 63) {
                throw new IOException("Unsupported timestamp resolution (" + resolution + ")");
            }
            return 1 << (resolution & 0x7F);
        } else {
            //MSB is 0 -> power of 10
            long result = 1;
            while(resolution-- > 0) {
                result *= 10;
            }
            return result;
        }
    }

    protected Map<Integer, ByteBuffer> parseOptions(final ByteBuffer bufOptions) {
        HashMap<Integer, ByteBuffer> mapOptions = new HashMap<>();
        if(!bufOptions.hasRemaining()) {
            //If no options are present, we're already done.
            return mapOptions;
        }

        int codeOption;
        int lengthOption;
        final byte[] arrData = bufOptions.array();
        do {
            codeOption = bufOptions.getShort();
            lengthOption = bufOptions.getShort();
            ByteBuffer bufOption = ByteBuffer.wrap(arrData, bufOptions.position(), lengthOption);
            bufOption.order(bufOptions.order());

            mapOptions.put(codeOption, bufOption);
            bufOptions.position(bufOptions.position() + lengthOption);
        } while(codeOption != 0 && bufOptions.hasRemaining());

        return mapOptions;
    }

    protected void processIdb(SeekableByteChannel channel, int size) throws IOException {
        final int idBlock = idNextBlock++;
        final ByteBuffer buf = ByteBuffer.allocate(size);
        if(isLittleEndian) {
            buf.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        channel.read(buf);
        buf.position(0);
        final int typeLink = buf.getShort(0);
        final IPlugin.FactoryPacketHandler factory = this.config.factoryForNetworkType(typeLink);
        if(factory == null) {
            //TODO: Error
        } else {
            this.handlerForInterface.put(idBlock, factory.create(this.source, this.packetQueue));
        }
        buf.position(8);
        Map<Integer, ByteBuffer> options = parseOptions(buf);

        //if_tsresol
        if(options.containsKey(9)) {
            timestampResolutions.put(idBlock, divisorFromTsresol(options.get(9).get()));
        } else {
            timestampResolutions.put(idBlock, 1000000L);
        }
        //TODO: Support for if_tzone (10), but this can't be done yet since the format is not part of the standard as of this writing.
    }

    /**
     *
     * @param channel
     * @return The number of bytes to advance the channel to arrive at the next block.
     * @throws IOException
     */
    protected int processBlockHeader(SeekableByteChannel channel) throws IOException {
        //Read the first 8 bytes of the header
        final byte[] header = new byte[8];
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        final int cbRead = channel.read(buffer);
        //end of stream
        if(cbRead == -1) {
            return -1;
        } else if(cbRead != 8) {
            throw new IOException("Unable to read start of PcapNg Block Header.");
        }

        final int typeBlock = buffer.getInt(0);//intFromBytes(header, 0, 4, isLittleEndian);
        final int sizeBlock = buffer.getInt(4);//intFromBytes(header, 4, 4, isLittleEndian);

        //If we're reading a SHB, then we don't know the endianness, so the size may be wrong, so we should skip the integrity check.
        if(typeBlock != 0x0A0D0D0A) {
            if((sizeBlock & 3) != 0) {
                throw new IOException("PcapNg contains invalid block size(" + sizeBlock + " / 0x" + Integer.toHexString(sizeBlock) + ")");
            }

            if(typeBlock < 0) {
                return sizeBlock - 8;
            }
        }

        final ByteBuffer bufPacket;
        final int cbProcessed;

        switch(typeBlock) {
            case 1: // Interface Description Block
                //processIdb expects the length to include the options but not the trailing size, so omit it here, and return the 4 bytes to advance past the trailing size field.
                processIdb(channel, sizeBlock - 12);
                this.source.recordProgress(sizeBlock);
                return 4;
            //Skip these blocks
            case 5: //Interface statistics Block
            case 7: //IRIG Timestamp Block
            case 8: //ARINC 429 in AFDX Encapsulation Block
                this.source.recordProgress(sizeBlock);
                return sizeBlock - 8;
            case 3: //Simple Packet Block
                //TODO: Better handling of the padding bytes and truncated packets.
                //The next 4 bytes contains the original packet length
                channel.position(channel.position() + 4);
                bufPacket = ByteBuffer.allocateDirect(sizeBlock - 16);
                channel.read(bufPacket);
                bufPacket.rewind();
                cbProcessed = handlerForInterface.get(0).handle(bufPacket, Instant.now().toEpochMilli(), idxFrame++);

                //Record progress for the non-packet size of the block, then return 4 to advance past the trailing block size.
                this.source.recordProgress(sizeBlock - cbProcessed);
                return 4;
            case 2: //Packet Block (Obsolete, but should still be readable)
                //The only difference between 2 and 6 is that the interfaceID is 2 bytes (instead of 4) in 2, with the following 2 bytes for the Drops Count (which is not present in 6).
            case 6: //Enhanced Packet Block
                bufEnhancedHeader.rewind();
                channel.read(bufEnhancedHeader);

                final int idInterface;
                if(typeBlock == 2) {
                    idInterface = bufEnhancedHeader.getShort(0);
                } else {
                    idInterface = bufEnhancedHeader.getInt(0);
                }
                final long ts = ((long)bufEnhancedHeader.getInt(4) << 32) | ((long)bufEnhancedHeader.getInt(8) & 0x00000000FFFFFFFFL);
                final int cbCapture = (bufEnhancedHeader.getInt(12) + 3) & ~0x3; //Round up to the next multiple of 32 bits.

                bufPacket = ByteBuffer.wrap(new byte[cbCapture]);
                channel.read(bufPacket);
                bufPacket.rewind();
                cbProcessed = handlerForInterface.get(idInterface).handle(bufPacket, ts * 1000L / timestampResolutions.get(idInterface), idxFrame++);

                //There may be variable length options after the packet contents, so calculate the size to skip.
                this.source.recordProgress(sizeBlock - cbProcessed);
                //In the old code, this was using cbProcessed instead of cbCapture.  I have no idea how that code worked.
                //UPDATE: It wasn't working.  That was a bug in 3.2
                return sizeBlock - (28 + cbCapture);
            case 4: //Name Resolution Block
                //TODO: Parse the NRB and report names for logical addresses.
                this.source.recordProgress(sizeBlock);
                return sizeBlock - 8;
            case 0x0A0D0D0A:
                final ByteBuffer blockSectionHeader = ByteBuffer.wrap(new byte[4]);
                channel.read(blockSectionHeader);

                final int magicNumber = blockSectionHeader.getInt(0);
                if(magicNumber == 0x1A2B3C4D) {
                    isLittleEndian = false;
                    bufEnhancedHeader.order(ByteOrder.BIG_ENDIAN);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                } else if(magicNumber == 0x4D3C2B1A) {
                    isLittleEndian = true;
                    bufEnhancedHeader.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                } else {
                    throw new IOException("BOM Field in Section Header block is wrong(0x" + Integer.toHexString(magicNumber) + ")");
                }

                final int sizeShb = buffer.getInt(4);
                this.source.recordProgress(sizeShb);
                return sizeShb - 12;
            //Error conditions of varying severity
            case 0x00000BAD:
            case 0x40000BAD:
                Logger.log(Logger.Severity.WARNING, "PcapNg File contains unparsable data (" + sizeBlock + " bytes)");
                this.source.recordProgress(sizeBlock);
                return sizeBlock - 8;
            case 0:
            default:
                throw new IOException("Unknown block type: 0x" + ("0000000" + Integer.toHexString(typeBlock)).replaceAll("^.*(?=.{8}$)", ""));
        }
    }

    protected void parseSource() throws IllegalStateException {
        done = false;

        Thread loopThread = new Thread(() -> {
            source.importStartedProperty().set(true);
            try (SeekableByteChannel reader = Files.newByteChannel(source.getPath())) {
                while (true) {
                    long cbRemaining = (long) processBlockHeader(reader);

                    if (cbRemaining == -1) {
                        //End-of-stream
                        break;
                    } else if (cbRemaining != 0) {
                        //Advance by given length
                        reader.position(reader.position() + cbRemaining);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                done = true;
                source.importCompleteProperty().set(true);
            }
        }, "PcapNg Loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public Iterator<Object> getIterator() {
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return !(done && packetQueue.isEmpty());
            }

            @Override
            public Object next() {
                return packetQueue.poll();
            }

            @Override
            public String toString() {
                return PcapNgImport.this.source.toString();
            }
        };
    }

    @Override
    public String toString() {
        return String.format("%s, (%s%f%%)", this.source, this.done ? "COMPLETE / " : "", 100.0 * this.source.progressProperty().doubleValue());
    }
}
