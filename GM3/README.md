# Build Instructions
```
mvn generate-resources
mvn package
```

# Run Instruction
```
java -jar target/grassmarlin-cli-1.0-SNAPSHOT-jar-with-dependencies.jar -f <fingerprints> -p <pcap>
```

# PCAP parsing
GRASSMARLIN requires the use of jnetpcap to perform pcap parsing. This library
can be found at the following link:
https://downloads.sourceforge.net/project/jnetpcap/jnetpcap/Latest/jnetpcap-1.4.r1425-1.linux64.x86_64.tgz
extract this tarball and copy the `.so` files to `/usr/lib/x86_64-linux-gnu/`
```
tar xvf jnetpcap-1.4.r1425-1.linux64.x86_64.tgz
cd jnetpcap-1.4.r1425-1.linux64.x86_64
cp libjnetpcap* /usr/lib/x86_64-linux-gnu/
```
