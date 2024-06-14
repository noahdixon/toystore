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
for ((step=0; step<=10; step=step+2)); do

    # Calculate probability based on step
    probability=$(awk "BEGIN {printf \"%.2f\", $step / 10}")

    # Create the folder with inside the 'out' directory
    output_folder="$out_directory/out_step_${step}"

    mkdir -p "$output_folder"

    for ((client_i=1; client_i<=5; client_i++)); do
        java -cp "./client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main ${req:+-req} "$req" ${server_address:+-ser} "$server_address" ${port:+-p} "$port" -pr "$probability" -l > "$output_folder"/latencies_${client_i}.txt &
    done

    # Wait for background processes to complete in this for loop
    wait
done

# Waiting for any remaining processes to complete
wait

echo "All clients have finished running"