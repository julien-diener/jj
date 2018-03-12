just playing with distributed app concept

Server: run a Server that listen for ping and bye message
AppTest: start couple of Server, either in Thread or external Thread, then ping and close connection

todo:
 - doesn't work when with external server are started in sbt
 - each Server should have its own classpath
 - start cluster with docker
