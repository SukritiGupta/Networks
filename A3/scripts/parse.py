import pandas as pd 
import numpy as np 
from pylab import *
import matplotlib.pyplot as plt 


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


df = pd.read_csv("lbnl.anon-ftp.03-01-11.csv", index_col="No.")

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

#2
no_of_flows = tcp_packets["Flow_id"].unique().size

#3
#assuming number of tcp connections opened to the server is equal to number of SYN packets send to it.
srvr_ip = servers[1]							#select the syn packets of any particular server at random
cnctn_to_srvr = syn_packets[syn_packets["Destination"]==srvr_ip]
profile = daily_profile(cnctn_to_srvr["Time"])	


#4
fin_packets = tcp_packets[tcp_packets["Info"].str.contains("FIN")]
rst_packets = tcp_packets[tcp_packets["Info"].str.contains("RST")]


#for each packet in syn packet look for chronologically next packet exchanged b/w client-server in all_pac list
# if it is an  SYN packet then we know that this connection was never established properly hence reconnection is being done,
#however if it is RST or FIN then we measure the time for which the connection was open

def look(l,t,s):
	s1 = rev_id(s)
	l = l[l["Time"]>t]
	for index, row in l.iterrows():
		if(row["Flow_id"]==s or row["Flow_id"]==s1):
			w = req_type(row["Info"])
			if w<=1 :
				return -1
			else:
				return (row["Time"]-t)
 


open_time=[]
all_pac = tcp_packets[tcp_packets["Info"].str.contains("\[SYN\]|RST|FIN")]

for index, row in syn_packets.iterrows():
	w = look(all_pac, row["Time"], row["Flow_id"])
	if w>=0:
		open_time.append(w)

open_time.sort()
y=[]
u=0
for i in range(0,len(open_time)):
	y.append(u)
	u=u+1


y=[(float(k)/len(open_time)) for k in y]

plt.plot(open_time,y)
plt.xlabel('Connection Duration (sec)', fontsize=18)
plt.ylabel('P(Connection duration < X)',fontsize=16)
plt.show()

