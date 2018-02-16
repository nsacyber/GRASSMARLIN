package grassmarlin.plugins.internal.offlinepcap;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PcapImport {
    private final ImportItem source;
    private final BlockingQueue<Object> packetQueue;
    private final RuntimeConfiguration config;
    private boolean done;

    public PcapImport(final RuntimeConfiguration config, final ImportItem source) {
        this.source = source;
        this.packetQueue = new ArrayBlockingQueue<>(100);
        this.config = config;
        this.done = false;

        if(!validateFileFormat(this.source.getPath(), true)) {
            throw new IllegalArgumentException("File failed validation.");
        }

        parseSource();
    }

    public static boolean validateFileFormat(final Path path, final boolean checkForPcapNg) {
        //TODO: The previous iteration contained a hack workaround for handling the live pcap dump files--we don't need that anymore, right?
        try {
            //We need at least 24 for a valid pcap, but only 12 for the relevant part of the pcapng header.  We rely on BOM failing for an invalid file.
            if(!Files.exists(path) || Files.size(path) < 12L) {
                return false;
            }
        } catch(IOException ex) {
            return false;
        }

        try(SeekableByteChannel reader = Files.newByteChannel(path)) {
            final ByteBuffer buffer = ByteBuffer.allocate(12);
            reader.read(buffer);
            int bom = buffer.getInt(0);
            if(bom == 0xA1B2C3D4 || bom == 0xD4C3B2A1) {
                return true;
            }

            if(checkForPcapNg) {
                //At this point we know this is not a pcap file, but check for the pcapng BOM field.  If it matches, output a notice to the user suggesting the change.
                bom = buffer.getInt(8);
                if (bom == 0x1A2B3C4D || bom == 0x4D3C2B1A) {
                    Logger.log(Logger.Severity.WARNING, "The file appears to be a PcapNg file, not a Pcap file (%s)", path);
                }
            }
            return false;
        } catch(IOException ex) {
            return false;
        }
    }

    protected void parseSource() {
        done = false;

        final Thread loop = new Thread(() -> {
            source.importStartedProperty().set(true);
            int idxFrame = 1;
            try(ByteChannel reader = Files.newByteChannel(source.getPath())) {
                //Read Header
                final ByteBuffer buffer = ByteBuffer.allocate(24);
                buffer.mark();
                reader.read(buffer);
                this.source.recordProgress(24L);

                //TODO: Re-evaluate whether we should be setting endianness or porting in endianness-sensitive support methods.
                final boolean isSwapped;
                if(buffer.get(0) == -95 && buffer.get(1) == -78 && buffer.get(2) == -61 && buffer.get(3) == -44) {
                    isSwapped = true;
                    buffer.order(ByteOrder.BIG_ENDIAN);
                } else if(buffer.get(0) == -44 && buffer.get(1) == -61 && buffer.get(2) == -78 && buffer.get(3) == -95) {
                    isSwapped = false;
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                } else {
                    //Invalid header
                    return;
                }

                final long secGmtOffset = buffer.getInt(8);
                final int typeNetwork = buffer.getInt(20);

                final IPlugin.FactoryPacketHandler factory = this.config.factoryForNetworkType(typeNetwork);
                if(factory == null) {
                    Logger.log(Logger.Severity.ERROR, "Unable to process import of network type [%d]; no handler is configured to process this type.", typeNetwork);
                    return;
                }
                final IPlugin.IPacketHandler handler = factory.create(source, packetQueue);

                buffer.reset(); //We previously marked at the start, so this is akin to a rewind, but might be a tiny, tiny bit faster.
                buffer.limit(16); //The per-packet header is 16 bytes, as opposed to the 24 for the file header.

                final ByteBuffer bufferReusable = ByteBuffer.allocateDirect(2048);

                while(16 == reader.read(buffer)) {
                    final long sTimestamp = buffer.getInt(0);
                    final long usTimestamp = buffer.getInt(4);
                    final int lengthPacket = buffer.getInt(8);

                    final ByteBuffer contentsPacket;
                    if(lengthPacket <= 2048) {
                        contentsPacket = bufferReusable;
                        contentsPacket.rewind().limit(lengthPacket);
                    } else {
                        contentsPacket = ByteBuffer.allocateDirect(lengthPacket);
                    }

                    if(reader.read(contentsPacket) != lengthPacket) {
                        return;
                        //TODO: Reassess error reporting here.
                    }
                    contentsPacket.rewind();

                    final int cbProcessed = handler.handle(contentsPacket, (sTimestamp + secGmtOffset) * 1000L + usTimestamp / 1000L, idxFrame++);
                    this.source.recordProgress(lengthPacket + 16 - cbProcessed);
                    buffer.rewind();
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                done = true;
                source.importCompleteProperty().set(true);
            }
        }, "PcapLoop/" + this.toString());
        loop.setDaemon(true);
        loop.start();
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
                return PcapImport.this.source.toString();
            }
        };
    }

    @Override
    public String toString() {
        return String.format("%s, (%s%f%%)", this.source, this.done ? "COMPLETE / " : "", 100.0 * this.source.progressProperty().doubleValue());
    }
}
