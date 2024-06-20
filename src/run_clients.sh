#!/bin/bash

# Default values for command line arguments
server_address=""
port=""
req=""

# Read command line arguments
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -h|-host)
            server_address="$2"
            shift 2
            ;;
        -p|-port)
            port="$2"
            shift 2
            ;;
        -r|-req)
            req="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $key"
            exit 1
            ;;
    esac
done


# Create the 'out' directory if it does not exist
out_directory="out"
rm -rf "$out_directory"
mkdir -p "$out_directory"

# Start clients sequentially for each probability number
for ((clients=1; clients<=10; clients++)); do

    # Create the folder with inside the 'out' directory
    output_folder="$out_directory/clients_${clients}"

    mkdir -p "$output_folder"

    echo "Running $clients concurrent clients"

    for ((client_i=1; client_i<=clients; client_i++)); do
        java -cp "client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main ${server_address:+-ser "$server_address"} ${port:+-p "$port"} ${req:+-req "$req"}  -pr 1 -l > "$output_folder"/latencies_${client_i}.txt &
    done

    # Wait for background processes to complete in this for loop
    wait
done

# Waiting for any remaining processes to complete
wait

echo "All clients finished running"