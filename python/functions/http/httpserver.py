#!/usr/bin/python
# -*- coding: UTF-8 -*-

import urllib  # 用于对URL进行编解码
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler  # 导入HTTP处理相关的模块


# 自定义处理程序，用于处理HTTP请求
class TestHTTPHandler(BaseHTTPRequestHandler):
    # 处理GET请求
    def do_GET(self):
        path, args = urllib.splitquery(self.path)
        datas = 'None'
        if 'content-length' in self.headers:
            datas = self.rfile.read(int(self.headers['content-length']))
        args = "None" if args == None else args
        datas = 'None' if datas == None else datas
        print 'Path=', path
        print 'Args=', args
        print 'Data=', datas
        self.output()

    def do_POST(self):
        path, args = urllib.splitquery(self.path)
        datas = 'None'
        if 'content-length' in self.headers:
            datas = self.rfile.read(int(self.headers['content-length']))
        args = "None" if args == None else args
        datas = 'None' if datas == None else datas
        print 'Path=', path
        print 'Args=', args
        print 'Data=', datas
        self.output()

    def output(self):
        # 页面输出模板字符串
        templateStr = '''
        <html>   
        <head>   
        <title>Http Test</title>   
        </head>   
        <body>   
        Hello!
        </body>   
        </html>
        '''
        self.protocal_version = 'HTTP/1.1'  # 设置协议版本
        self.send_response(200)  # 设置响应状态码
        self.send_header("Welcome", "Contect")  # 设置响应头
        self.end_headers()
        self.wfile.write(templateStr)  # 输出响应内容

 # 启动服务函数


def start_server(port):
    http_server = HTTPServer(('', int(port)), TestHTTPHandler)
    http_server.serve_forever()  # 设置一直监听并接收请求


start_server(8000)  # 启动服务，监听8000端口
