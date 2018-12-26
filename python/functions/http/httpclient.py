from httplib2 import Http
ip = "xxxx"
url = "http://"+ip+":12584/cii/ds/login"
data = "{\"name\":\"xxxx\",\"pwd\":\"xxxx\"}"
http = Http()
resp, content = http.request(url, "POST", data)
headers = {"Cookie": resp['set-cookie']}

url = "http://"+ip+":12584/cii/da/job/listJob?service_id=204"
resp, content = http.request(url, method='GET', headers=headers)
print(content.decode("utf-8"))

url = "http://127.0.0.1:8000"
resp, content = http.request(url, method='GET', body=data)
print(content.decode("utf-8"))
