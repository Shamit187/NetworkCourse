import sys
import matplotlib.pyplot as plt

n = len(sys.argv)
sys.argv[0]

# line data
# throughput, delay, delivery ratio, drop ratio
map_data = ["throughput", "delay", "delivery ratio", "drop ratio"]

output = open(sys.argv[1], 'r')
data = output.readlines()

size = len(data) / 2
size = int(size)

throughput = []
delay = []
delratio = []
dropratio = []
variable = []

for i in range(size):
    string_data = data[2*i]
    variable.append(int(string_data))

    string_data = data[2*i + 1]
    string_data = string_data.split()
    throughput.append(float(string_data[0])) 
    delay.append(float(string_data[1])) 
    delratio.append(float(string_data[2])) 
    dropratio.append(float(string_data[3])) 

lists = [throughput, delay, delratio, dropratio]

for i in range(4):
    plt.plot(variable, lists[i])
    plt.xlabel(sys.argv[2])
    plt.ylabel(map_data[i])
    plt.savefig(sys.argv[2]+"/"+map_data[i]+".jpg")
    plt.close()

