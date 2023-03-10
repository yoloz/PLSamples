import hashlib
import collections
import psutil as ps 

def xyeneiey():
 d=collections.OrderedDict()
 #d["cpu"] = psutil.cpu_count()
 #d["mem"] = psutil.virtual_memory()[0]
 #d["swap"] =  psutil.swap_memory()[0]
 #d["disks"] = psutil.disk_partitions()
 d["net"] =[]
 net = ps.net_if_addrs()
#  v_nets = get_virtual_net()
 for name,values in net.items():
  # if name not in v_nets:
   for netio in values:
    if netio[0]==17:
     #print(netio)
     d["net"].append(netio[1])
 #排序网卡顺序
 d["net"].sort()
 rowK = d.__str__()
 print(rowK)
 #add 第一次启动的时间
 import os,time 
 ftime = time.time()
 #add by gjw on 2021-0128 固定PK
 ftime = int(ftime)
 if not os.path.exists("/home/yoloz/test/ft.log"):
  os.makedirs("/home/yoloz/test/",exist_ok=True)
  with open('/home/yoloz/test/ft.log', 'w+') as f:
   f.write("%d"%(ftime))
 else:
  with open('/home/yoloz/test/ft.log', 'r') as f:
   ftime = int(f.read())
 rowK = "%s-%s"%(rowK,ftime)
 print(rowK)
 m = hashlib.md5()
 #support python3
 m.update(rowK.encode("utf8"))
 #m.update(rowK)
 pk = m.hexdigest()
#  put_key("PK",pk)
 return pk

def yyy(x,y):
#  x = get_key("PK")
#  y = get_key("SN")
 if y=="":return -1,1
 if zzz(x,y,"0"): return 0,2
 if zzz(x,y,"1"): return 1,2
 if zzz(x,y,"2"):return 2,3
 if zzz(x,y,"3"):return 3,3
 if zzz(x,y,"4"):return 4,4
 if zzz(x,y,"5"):return 5,5
 if zzz(x,y,"6"):return 6,8
 if zzz(x,y,"7"):return 7,10
 if zzz(x,y,"8"):return 8,20
 return -1,1

def zzz(x,y,p):
 x = x+p
 h = hashlib.sha1()
 z = x[1]+x[24:]+x[2:9]+x[12:18]+x[20:24]
 h.update(z.encode("utf8"))
 if y==h.hexdigest()[4:-4]:
  return True
 return False

# print(xyeneiey())

print(yyy('aa33e1cc63db80cd7e54ac3e1bbdc3f0','3bea70261bc046224900a3ffe4b8de6c'))