import pandas as pd
import matplotlib.pyplot as plt
import sys
def rev_id(s):
	l = s.strip('()').split(", ")
	return "("+l[1]+", "+l[0]+", "+l[3]+", "+l[2]+")"

df = pd.read_csv("../Database/lbnl.anon-ftp.03-01-14.csv", index_col="No.")

tcp_packets = df[df["Protocol"]=="TCP"]

port_info = tcp_packets["Info"].str.split("[",n=1,expand=True)[0]
src_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[0])
dest_port = pd.to_numeric(port_info.str.split(">",n=1,expand=True)[1])

tcp_packets["src_port"] = src_port.to_list()
tcp_packets["dest_port"] = dest_port.to_list()
tcp_packets["Flow_id"] = "("+tcp_packets['Source']+", "+tcp_packets['Destination']+", "+tcp_packets['src_port'].astype(str)+", "+tcp_packets['dest_port'].astype(str)+")"

syn_packets = tcp_packets[tcp_packets["Info"].str.contains("SYN")]
syn_packets = syn_packets[~syn_packets["Info"].str.contains("\[SYN, ACK\]")]

clients = syn_packets["Source"].unique()
servers = syn_packets["Destination"].unique()

seen=[]
for i in range(0,len(tcp_packets)):
	if ((tcp_packets.iloc[i]["Destination"] in servers) and (not( tcp_packets.iloc[i]["Destination"] in seen))):
		tofind=tcp_packets.iloc[i]["Flow_id"]
		fromserverToC=tcp_packets[tcp_packets["Flow_id"]==tofind]
		seen.append(tcp_packets.iloc[i]["Destination"])
		tofind_rev=rev_id(tofind)
		fromclientToS=tcp_packets[tcp_packets["Flow_id"]==tofind_rev]

		seqnum=fromserverToC["Info"].str.split("Seq=",n=1,expand=True)[1]
		seqnum=seqnum.str.split(" ",n=1,expand=True)[0]
		fromserverToC["seqnum"]=seqnum

		seqnumstc=fromclientToS["Info"].str.split("Seq=",n=1,expand=True)[1]
		seqnumstc=seqnumstc.str.split(" ",n=1,expand=True)[0]
		fromclientToS["seqnumstc"]=seqnumstc

		ackno=fromclientToS["Info"].str.split("Ack=",n=1,expand=True)[1]
		ackno=ackno.str.split(" ",n=1,expand=True)[0]
		fromclientToS["ackno"]=ackno
		if ((len(ackno.unique())!=len(ackno)) and (len(ackno))<20 and (len(ackno))>5) :
			break
print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
print(tofind)
print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

ackedseq=[]
usefulxstc=list(fromserverToC["Time"])
usefulystc=list(seqnum)
usefulx=[]
usefuly=[]
for i in range(0,len(fromclientToS)):
	if(not(fromclientToS["ackno"].iloc[i] in ackedseq)):
		# ackedseq.append(fromclientToS["ackno"].iloc[i])
		usefulx.append(fromclientToS["Time"].iloc[i])
		usefuly.append(fromclientToS["seqnumstc"].iloc[i])

# plt.scatter(usefulx,usefuly)
# plt.title("Seqnum vs Time")
# plt.xlabel('Time (sec)', fontsize=14)
# plt.ylabel('Seqnum',fontsize=14)
# plt.show()


g1 = (usefulxstc,usefulystc)
g2 = (usefulx,usefuly)

data = (g1, g2)
colors = ("red", "green")
groups = ("Sequence Number S to C", "Seq of Ack C to S")

# Create plot
fig = plt.figure()
ax = fig.add_subplot(1, 1, 1, axisbg="1.0")

for data, color, group in zip(data, colors, groups):
	x, y = data
	ax.scatter(x, y, alpha=0.8, c=color, edgecolors='none', s=30, label=group)

plt.title('Matplot scatter plot')
plt.legend(loc=4)
plt.show()

