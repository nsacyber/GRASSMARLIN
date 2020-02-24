#!/usr/bin/env python3

import argparse
import subprocess

def main(args):
    # java_path = "/usr/lib/jvm/java-11-openjdk-amd64/bin/java"
    java_path = args.java
    jar_name = "target/minow-1.0-SNAPSHOT-jar-with-dependencies.jar"
    fingerprint_dir = args.fingerprints
    pcap_file = args.pcap

    gm_cmd = [java_path, '-jar', jar_name, '-f', fingerprint_dir, '-p', pcap_file]
    proc = subprocess.run(gm_cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, universal_newlines=True)
    for xml_elt_string in proc.stdout.split('\n'):
        print(xml_elt_string)
    # print(proc.stdout)


if __name__ == "__main__":
    parser = argparse.ArgumentParser('Python wrapper for the commandline Version of GRASSMARLIN')
    parser.add_argument('-f', '--fingerprints', required=True,
            help = 'directory of GRASMARLIN style fingerprint xml files')
    parser.add_argument('-p', '--pcap', required=True,
            help = 'pcap file for GRASMARLIN to parse')
    parser.add_argument('-j', '--java', default = '/bin/java',
            help='location of java executable to use')

    args = parser.parse_args()
    main(args)
