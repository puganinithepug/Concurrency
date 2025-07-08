#!/usr/bin/env bash

# Default values if no input is provided
w=${1:-1024}   # Width of output image
h=${2:-1024}   # Height of output image
n=100  # Fixed number of snowmen
t_max=${3:-100}
# Compile the Java program
javac DrawSnowmenConcurrently.java

# Output CSV file
output_file="data.csv"

# Create the CSV file with header
echo "Threads,Single-threaded Time,Multi-threaded Time" > $output_file

# Check if user has provided a value for t_max or thread values
if [ -z "$3" ]; then
    # Default case: Loop over predefined thread values (1, 20, 40, 60, 80, 100)
    for t in 1 20 40 60 80 100
    do
        echo "Running with $t threads..."
        
        # Measure time for the single-threaded case
        single_thread_time=$(java DrawSnowmenConcurrently $w $h $t $n 1 | grep -oP '\d+(?= ms)')
        
        # Measure time for the multi-threaded case
        multi_thread_time=$(java DrawSnowmenConcurrently $w $h $t $n $t | grep -oP '\d+(?= ms)')
        
        # Format the output and append to the CSV file
        echo "$t,Time to execute: $single_thread_time ms,Time to execute: $multi_thread_time ms" >> $output_file
    done
else
    # User has provided input for t_max, loop 6 times with random values for t
    thread_values=()

    # Generate 6 random values for t and store them in an array
    for i in {1..6}
    do
        t=$(( RANDOM % (t_max-10) + 11 ))
        thread_values+=($t)
    done

    # Sort the thread values in increasing order
sorted_thread_values=($(printf "%s\n" "${thread_values[@]}" | sort -n))

    # Loop over the sorted thread values
    for t in "${sorted_thread_values[@]}"
    do
        # Measure time for the single-threaded case
        single_thread_time=$(java DrawSnowmenConcurrently $w $h $t $n 1 | grep -oP '\d+(?= ms)')

        # Measure time for the multi-threaded case
        multi_thread_time=$(java DrawSnowmenConcurrently $w $h $t $n $t | grep -oP '\d+(?= ms)')

        # Format the output and append to the CSV file
        echo "$t,Time to execute: $single_thread_time ms,Time to execute: $multi_thread_time ms" >> $output_file
    done
fi

echo "Data collection complete. Output written to $output_file."
