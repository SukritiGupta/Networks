from __future__ import print_function
import pandas as pd 
import matplotlib.pyplot as plt 
from statistics import median, mean 

df = pd.read_csv("../Database/lbnl.anon-ftp.03-01-18.csv", index_col="No.")

tcp_packets = df[df["Protocol"]=="TCP"]

port_info = tcp_packets["Info"].str.split("[",n=1,expand=True)[0]
src_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[0])
dest_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[1])

tcp_packets["src_port"] = src_port.to_list()
tcp_packets["dest_port"] = dest_port.to_list()
tcp_packets["Flow_id"] = "("+tcp_packets['Source']+", "+tcp_packets['Destination']+", "+tcp_packets['src_port'].astype(str)+", "+tcp_packets['dest_port'].astype(str)+")"

syn_packets = tcp_packets[tcp_packets["Info"].str.contains("SYN")]
syn_packets = syn_packets[~syn_packets["Info"].str.contains("\[SYN, ACK\]")]


# temp=list(syn_packets["Time"])
# interarrival_time=[(temp[i]-temp[i-1]) for i in range(1,len(temp))]
# interarrival_time.sort()
# y_it=[]
# u_it=0
# for i in range(0,len(interarrival_time)):
# 	y_it.append(u_it)
# 	u_it=u_it+1


# y_it=[(float(k)/len(interarrival_time)) for k in y_it]

# plt.plot(interarrival_time,y_it)
# plt.title("CDF of interarrival time between connection requests")
# plt.xlabel('Gap between two connection requests (sec)', fontsize=14)
# plt.ylabel('P(Gap < X)',fontsize=14)
# plt.show()

# print("Mean of Interarrival Time: "+str(mean(interarrival_time)))
# print("Median of Interarrival Time: "+str(median(interarrival_time)))

incoming_packets=[]
incoming_packets_size=[]
outgoing_packets_size=[]
servers = syn_packets["Destination"].unique()

for i in range(0,len(tcp_packets)):
	if (tcp_packets.iloc[i]["Destination"] in servers):
		incoming_packets.append(tcp_packets.iloc[i]["Time"])
		incoming_packets_size.append(tcp_packets.iloc[i]["Length"])
	else :
		outgoing_packets_size.append(tcp_packets.iloc[i]["Length"])

print("Mean of Incoming packets Size: "+str(mean(incoming_packets_size)))
# print("Median of Incoming packets Size: "+str(median(interarrival_time)))
exit()

temp=incoming_packets
interarrival_time=[(temp[i]-temp[i-1]) for i in range(1,len(temp))]
interarrival_time.sort()
y_it=[]
u_it=0
for i in range(0,len(interarrival_time)):
	y_it.append(u_it)
	u_it=u_it+1


y_it=[(float(k)/len(interarrival_time)) for k in y_it]

plt.plot(interarrival_time,y_it)
plt.title("CDF of interarrival time of all pkts")
plt.xlabel('Gap between two incoming packets to servers (sec)', fontsize=14)
plt.ylabel('P(Gap < X)',fontsize=14)
plt.show()

print("Mean of Interarrival Time incoming packets to server: "+str(mean(interarrival_time)))
print("Median of Interarrival Time incoming packets to server: "+str(median(interarrival_time)))





temp=incoming_packets_size
temp.sort()
y_it=[]
u_it=0
for i in range(0,len(temp)):
	y_it.append(u_it)
	u_it=u_it+1


y_it=[(float(k)/len(temp)) for k in y_it]

plt.plot(temp,y_it)
plt.title("CDF of incoming packet size")
plt.xlabel('Incoming Packet size', fontsize=14)
plt.ylabel('P(Size < X)',fontsize=14)
plt.show()

temp=outgoing_packets_size
temp.sort()
y_it=[]
u_it=0
for i in range(0,len(temp)):
	y_it.append(u_it)
	u_it=u_it+1


y_it=[(float(k)/len(temp)) for k in y_it]

plt.plot(temp,y_it)
plt.title("CDF of Outgoing packet size")
plt.xlabel('Outgoing Packet size', fontsize=14)
plt.ylabel('P(Size < X)',fontsize=14)
plt.show()
