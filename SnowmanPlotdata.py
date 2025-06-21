import matplotlib.pyplot as plt
import numpy as np
import csv

# Load the data from CSV file
data = []
with open('data.csv', 'r') as f:
    reader = csv.reader(f)
    next(reader)  # Skip the header row
    for row in reader:
        # Extract thread count, single-thread time, and multi-thread time
        threads = int(row[0])
        single_thread_time = int(row[1].split()[3])  # Extract the number from the string 'Time to execute: X ms'
        multi_thread_time = int(row[2].split()[3])  # Extract the number from the string 'Time to execute: X ms'
        
        # Append the data as a tuple
        data.append([threads, single_thread_time, multi_thread_time])

# Extract the relevant columns for plotting
threads = [row[0] for row in data]
single_thread_time = [row[1] for row in data]
multi_thread_time = [row[2] for row in data]

# Calculate speedup: single-thread time divided by multi-thread time
speedup = [single / multi if multi != 0 else 0 for single, multi in zip(single_thread_time, multi_thread_time)]

# Plot total time vs threads
plt.figure(figsize=(12, 6))

# Subplot for total time vs threads
plt.subplot(1, 2, 1)
plt.plot(threads, single_thread_time, label='Single-threaded', marker='o', color='b')
plt.plot(threads, multi_thread_time, label='Multi-threaded', marker='o', color='r')
plt.xlabel('Number of Threads')
plt.ylabel('Execution Time (ms)')
plt.title('Total Time vs Threads')
plt.legend()

# Subplot for speedup vs threads
plt.subplot(1, 2, 2)
plt.plot(threads, speedup, label='Speedup', marker='o', color='g')
plt.xlabel('Number of Threads')
plt.ylabel('Speedup')
plt.title('Speedup vs Threads')

# Show the plots
plt.tight_layout()
plt.show()
