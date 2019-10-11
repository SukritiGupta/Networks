from __future__ import print_function
import pandas as pd 
import numpy as np 
from pylab import *
import matplotlib.pyplot as plt 
from statistics import median, mean 
from scipy.stats import pearsonr


def daily_profile(s):
	hr = 3600
	a=[]
	for i in range(0,24):
		a.append(cnctn_to_srvr["Time"].between(left=i*3600,right=(i+1)*3600).sum())
	return a

def rev_id(s):
	l = s.strip('()').split(", ")
	return "("+l[1]+", "+l[0]+", "+l[3]+", "+l[2]+")"

def req_type(s):
	if "[SYN]" in s:
		return 1
	elif (("RST" in s) or ("FIN" in s)):
		return 2
	else:
		return -1


df = pd.read_csv("../Database/lbnl.anon-ftp.03-01-14.csv", index_col="No.")

tcp_packets = df[df["Protocol"]=="TCP"]

port_info = tcp_packets["Info"].str.split("[",n=1,expand=True)[0]
src_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[0])
dest_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[1])

tcp_packets["src_port"] = src_port.to_list()
tcp_packets["dest_port"] = dest_port.to_list()
tcp_packets["Flow_id"] = "("+tcp_packets['Source']+", "+tcp_packets['Destination']+", "+tcp_packets['src_port'].astype(str)+", "+tcp_packets['dest_port'].astype(str)+")"


# I have  counted only packets  with [SYN];  we could have counted [SYN,ACK] (which are slighly more in number), there are also packets of kind [SYN, ECN, CWR]
syn_packets = tcp_packets[tcp_packets["Info"].str.contains("SYN")]
syn_packets = syn_packets[~syn_packets["Info"].str.contains("\[SYN, ACK\]")]

clients = syn_packets["Source"].unique()
servers = syn_packets["Destination"].unique()

#1
no_of_clients = syn_packets["Source"].unique().size
no_of_servers = syn_packets["Destination"].unique().size

print("No. of servers, No. of clients: "+str(no_of_servers)+", "+str(no_of_clients))


#2
no_of_flows = (tcp_packets["Flow_id"].unique().size)
print("No. of unique Flows: "+str(no_of_flows))

#3
#assuming number of tcp connections opened to the server is equal to number of SYN packets send to it.
srvr_ip = servers[1]							#select the syn packets of any particular server at random
cnctn_to_srvr = syn_packets[syn_packets["Destination"]==srvr_ip]
profile = daily_profile(cnctn_to_srvr["Time"])	
y_pos = np.arange(len(profile))
plt.bar(y_pos, profile )
plt.xlabel('Time of the Day(hrs)', fontsize=14)
plt.ylabel('No. of Connections opened',fontsize=14)
plt.show()


#4
#for each packet in syn packet look for chronologically next packet exchanged b/w client-server in all_pac list
# if it is an  SYN packet then we know that this connection was never established properly hence reconnection is being done,
#however if it is RST or FIN then we measure the time for which the connection was open

def look(l,t,s,tcp):
	s1 = rev_id(s)
	l = l[(l["Time"]>t) & ((l["Flow_id"]==s) | (l["Flow_id"]==s1))]
	if(l.size==0):
		tm=-1
	else:
		w = req_type(l.iloc[0]["Info"])
		if w<=1 :
			tm = -1
		else:
			tm = (l.iloc[0]["Time"]-t)
	rel=tcp[(tcp["Time"]<(t+tm)) & (tcp["Time"]>t) ]
	r1 = rel[rel["Flow_id"]==s]
	r2 = rel[rel["Flow_id"]==s1]
	return tm,sum(r1["Length"]),sum(r2["Length"])


open_time=[]
to_size=[]
fro_size=[]
all_pac = tcp_packets[tcp_packets["Info"].str.contains("SYN|RST|FIN")]
all_pac = all_pac[~all_pac["Info"].str.contains("\[SYN, ACK\]")]


for index, row in syn_packets.iterrows():
	w,s1,s2 = look(all_pac, row["Time"], row["Flow_id"],tcp_packets[(tcp_packets["Flow_id"]==row["Flow_id"]) | (tcp_packets["Flow_id"]==rev_id(row["Flow_id"])) ])
	if w>=0:
		open_time.append(w)
		to_size.append(s1)
		fro_size.append(s2)

median_time = median(open_time)
mean_time = mean(open_time)

print("Median,Mean of Time for which connection was open: "+str(median_time)+", "+str(mean_time ))


open_time_sorted=[]
for i in range(0,len(open_time)):
	open_time_sorted.append(open_time[i])


open_time_sorted.sort() 

y=[]
u=0
for i in range(0,len(open_time)):
	y.append(u)
	u=u+1


y=[(float(k)/len(open_time)) for k in y]

plt.plot(open_time_sorted,y)
plt.title("CDF of Connection Duration")
plt.xlabel('Connection Duration (sec)', fontsize=14)
plt.ylabel('P(Connection duration < X)',fontsize=14)
plt.show()

plt.scatter(open_time,to_size)
plt.title('No. of Bytes (sent to server) Vs. Connection Duration, Pearson_Coeff = '+str(pearsonr(open_time,to_size)[0]))
plt.xlabel('Connection Duration', fontsize=14)
plt.ylabel('No. of Bytes (sent to server)',fontsize=14)
plt.show()

plt.scatter(to_size,fro_size)
plt.title('No. of Bytes (sent to server) Vs. No. of Bytes (recvd from server), Pearson_Coeff = '+str(pearsonr(to_size,fro_size)[0]))
plt.xlabel('No. of Bytes (sent to server)', fontsize=14)
plt.ylabel('No. of Bytes (recvd from server)',fontsize=14)
plt.show()


temp=list(syn_packets["Time"])
interarrival_time=[(temp[i]-temp[i-1]) for i in range(1,len(temp))]
interarrival_time.sort()
y_it=[]
u_it=0
for i in range(0,len(interarrival_time)):
	y_it.append(u_it)
	u_it=u_it+1


y_it=[(float(k)/len(interarrival_time)) for k in y_it]

plt.plot(interarrival_time,y_it)
plt.title("CDF of interarrival time")
plt.xlabel('Gap between two connection requests (sec)', fontsize=14)
plt.ylabel('P(Gap < X)',fontsize=14)
plt.show()

print("Mean of Interarrival Time: "+str(mean(interarrival_time)))
print("Median of Interarrival Time: "+str(median(interarrival_time)))

incoming_packets=[]
incoming_packets_size=[]
outgoing_packets_size=[]
for i in range(0,len(tcp_packets)):
	if (tcp_packets.iloc[i]["Destination"] in servers):
		incoming_packets.append(tcp_packets.iloc[i]["Time"])
		incoming_packets_size.append(tcp_packets.iloc[i]["Length"])
	else :
		outgoing_packets_size.append(tcp_packets.iloc[i]["Length"])



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
plt.title("CDF of incoming packet size")
plt.xlabel('Incoming Packet size', fontsize=14)
plt.ylabel('P(Size < X)',fontsize=14)
plt.show()
