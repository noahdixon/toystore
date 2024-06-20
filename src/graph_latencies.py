import os
import numpy as np
import matplotlib.pyplot as plt

# Function to calculate average of 'query' and 'buy' latencies
def calculate_average(directory):
    query_latencies = []
    buy_latencies = []
    for file in os.listdir(directory):
        if file.endswith('.txt'):
            with open(os.path.join(directory, file), 'r') as f:
                line = f.readline()
                query, buy = map(float, line.strip().split(','))
                query_latencies.append(query)
                buy_latencies.append(buy)
    return np.mean(query_latencies), np.mean(buy_latencies)

def main():
    directory = 'out'
    avg_query_values = []
    avg_buy_values = []
    num_clients = list(range(1, 11))

    for step in num_clients:
        step_directory = os.path.join(directory, f'clients_{step}')
        avg_query, avg_buy = calculate_average(step_directory)
        avg_query_values.append(avg_query)
        avg_buy_values.append(avg_buy)

    # Generate plot
    plt.plot(num_clients, avg_query_values, label='Queries')
    plt.plot(num_clients, avg_buy_values, label='Buys')
    plt.xticks(num_clients)
    plt.xlabel('Number of Concurrent Clients')
    plt.ylabel('Average Latency (ms)')
    plt.title('Average Request Latency vs Number of Concurrent Clients')
    plt.legend()
    plt.grid(True)
    plt.savefig('latencies.png')

if __name__ == "__main__":
    main()
