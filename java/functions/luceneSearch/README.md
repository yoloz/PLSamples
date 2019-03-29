## 目录结构
```
project
│   README.md
│   build.gradle    
│   settings.gradle
└───bin
│   │   start.sh  #启动
│   │   stop.sh   #停止
│   │   checkEnv.sh
│   
└───conf
|   │   analyser.properties
|   │   server.properties
|   
└───src
    | 
    └───main
    |      │   ...
    |
    └───test
           │   ...  
```

## 配置文件说明
conf/server.properties
```
indexDir=.var    #索引创建目录,默认项目本地var
httpPort=12580   #工具对外api访问端口
pageCache=15     #单次搜索缓存静态多少页数据。返回需要的页数据，后台继续查询并缓存最多(能查询数据可能不够)pageCache-1页数据
searchCache=5    #索引的缓存搜索基数(LRU)。同一sql语句会覆盖。
totalIndex=6     #允许运行的索引实例数(LRU)。超过LRU原则关闭最旧的index实例，默认工具启动分配Xmx=1G
indexBuffer=128  #每个index实例的RAMBufferSize(MB)
perDayHour=2     #每天定时commit(0-23),避免长时间运行被断电等异常情况造成的损失
refreshTime=180  #实时搜索刷新(s)
```
> * 近实时搜索的index是动态的,工具提供了一次搜索的静态副本,一个索引的搜索缓存总数最大为totalIndex*searchCache,这种情况为只有一个index实例在运行;  
> * totalIndex实例数<(工具Xmx/indexBuffer)

conf/analyser.properties
```
StandardAnalyzer=org.apache.lucene.analysis.standard.StandardAnalyzer
```
> * key大小写不敏感;
> * 映射只对更新后的创建起作用,先前的已经创建的不影响；
> * 支持热更新,下文会有api；

## API说明
失败返回格式：{"success":false,"error":"errorMsg"}"

  url|说明
 :---:|:---:
 /create|创建
 /query|查询
 /start|启动
 /stop|停止
 /delAll|删除
 /updateAnalyser|更新分析器

* /create

POST indexName  
返回：{"success":true}

* /query

POST json {key:""\[,offset:,limit:\]}
1. {"key":"select * from test where city='hangzhou'"}
> 直接查询，支持 limit page,count,如：
select * from test where city='hangzhou' limit 1,15(取第一页数据15条)
select * from test where city='hangzhou' limit 1000(取前1000数据)
2. {"key":"gMSWyOmTgS2CjW0lM8Xz9A\u003d\u003d","offset":2,"limit":10}"
> 从静态副本中取数据(第二页10条)

返回：
1. 离线搜索(即索引已经停止)  
{"total":1000,"results":\[{}...\],"success":true}
2. 近实时搜索  
{"total":1000,"size":15,"results":[{}...],"key":"gt9OfOzANQgp/tLcgDXd3A\u003d\u003d","success":true}
> total为索引匹配的总数;  
size为静态副本数;  
key为静态副本key

* /start

POST indexName  
返回：{"success":true}

* /stop

POST indexName  
返回：{"success":true}

* /delAll

POST indexName  
返回：{"success":true}

* /updateAnalyser

POST  
返回：{"success":true}

## API用例

* 创建:
```
curl localhost:12580/create -X POST -d "CREATE TABLE test(index int,city string,company text,english text,time date('uuuu-MM-dd'T'HH:mm:ss.SSSSSS'),timestamp long) name=listTest addr='127.0.0.1:8888' type=list [analyser=StandardAnalyzer]"

curl localhost:12580/create -X POST -d "CREATE TABLE test(index int,city string,company text,english text,time date('uuuu-MM-dd'T'HH:mm:ss.SSSSSS'),timestamp long) name='list*' addr='127.0.0.1:8888' type=list"
```
> analyser:分词实现,可以不配置,默认是StandardAnalyzer,只需要填写analyser.properties配的键值即可;  
name:数据源名称,暂时只支持ssdb,name支持前缀匹配;  
addr:地址和端口;  
type:list或hash;  
字段类型:int,long,date,string,text.string不分词,text会分词;

* 停止:

`curl localhost:12580/stop -X POST -d "test"`create操作是不停止的,这个请求可以停止索引写和读并将数据写进磁盘;

* 启动:

`curl localhost:12580/start -X POST -d "test"`

* 删除:

`curl localhost:12580/delAll -X POST -d "test"`删除索引

* 搜索:
```
curl localhost:12580/query -X POST -d "{\"key\":\"select * from test where city='hangzhou' limit 0,10\"}"
curl localhost:12580/query -X POST -d "{\"key\":\"gMSWyOmTgS2CjW0lM8Xz9A\u003d\u003d\",\"offset\":1,\"limit\":10}"
curl localhost:12580/query -X POST -d "{\"key\":\"select city from test where company like '北' limit 1\"}"
```
> 支持条件and,or,like,=,>,>=,<,<=,between;  
对于条件>,>=,<,<=查询出的数据total不精确,限定用于时间和数字;
