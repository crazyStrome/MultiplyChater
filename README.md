# MultiplyChater
Java实现的多人聊天室，使用NIO和AIO实现服务器端代码。可以储存用户信息和ip：端口映射。

退出服务器时保存到本地的MySQL数据库中，开启服务时从数据库中加载用户信息，实现持久化存储。

##  文件结构
|--AIOClient.java<br>
|--CrazyStromeMap.java<br>
|--CrazyStromeProtocol.java<br>
|--MySQLExcutor.java<br>
|--NIOClient.java<br>
|--Server.java<br>

##  [AIOClient.java](https://github.com/crazyStrome/MultiplyChater/blob/master/src/crazyStrome/AIOClient.java)
使用异步IO和服务器建立连接，之后进行通信协议的解析和封装

##  [CrazyStromeMap.java](https://github.com/crazyStrome/MultiplyChater/blob/master/src/crazyStrome/CrazyStromeMap.java)
在内存中将用户信息和端口映射储存起来

##  [MySQLExecutor.java](https://github.com/crazyStrome/MultiplyChater/blob/master/src/crazyStrome/MySQLExecutor.java)
实现和MySQL的连接，开机读取数据库和关闭保存数据库。

##  [NIOClient.java](https://github.com/crazyStrome/MultiplyChater/blob/master/src/crazyStrome/NIOClient.java)
使用多路复用IO和服务器建立连接，之后进行通信协议的解析和封装

##  [Server.java](https://github.com/crazyStrome/MultiplyChater/blob/master/src/crazyStrome/Server.java)
使用NIO监听端口，处理服务。
