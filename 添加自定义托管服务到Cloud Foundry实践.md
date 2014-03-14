## 添加自定义托管服务到Cloud Foundry实践 #

**[Cloud Foundry](https://github.com/cloudfoundry)**，简称CF，是业界流行的[PaaS](http://en.wikipedia.org/wiki/Platform_as_a_service)平台。CF提供了应用隔离部署（基于轻量级容器[warden](http://docs.cloudfoundry.org/concepts/architecture/warden.html))，并且可以方便的集成[服务](http://docs.cloudfoundry.org/devguide/services/)。

### CF 服务介绍 ###

CF提供的服务可以分为2类：

 - 托管的服务: [Managed Service](http://docs.cloudfoundry.org/devguide/services/#managed-services)。这类服务由CF负责部署、安装、配置、启动等。当前CF官方支持的托管有：[MySQL](https://github.com/cloudfoundry/cf-services-release)，社区又添加了很多其他[常见服务](https://github.com/cloudfoundry-community/cf-services-contrib-release)的支持：包括[elasticsearch](www.elasticsearch.org/),[couchdb](http://couchdb.apache.org/), [neo4j](www.neo4j.org), [redis](http://redis.io/), [postgresql](www.postgresql.org/), [rabbitmq](https://www.rabbitmq.com/‎), [memcached](memcached.org), [mongodb](https://www.mongodb.org) 等。
 - 用户提供服务：[User-Provided Service](http://docs.cloudfoundry.org/devguide/services/user-provided.html)。 这类服务不适合受CF托管，自己独立安装部署，由CF来提供给连接信息（`credentials`）给应用，这样，应用不不需要 `hard-code` 这些`credentials`。
 
下面将描述如何将自定义服务作为托管的服务加入到CF中。

### 如何添加自定义服务 ###
CF的Service模块是一个非常松耦合的架构。只需要实现[5个Rest接口](http://docs.cloudfoundry.org/services/api.html)，就可以插入到CF中，作为一个Service Broker。Service Broker 一般分为2个部分：
 
 - Service Gateway : 负责实现5个Rest接口，选择合适的`Service Node`创建服务、绑定服务
 - Service Node：负责安装启动具体的服务、绑定服务并且返回`credentials`
 
当前，我们用 `java` 实现了较为通用的`Service Gateway`	和 `Service Node`。只需要稍加修改就可以适配其他的服务。

### 自定义Service Gateway ###

`Service Gateway` 基于`Spring MVC`实现，需要运行在web容器中，建议使用 [jetty](http://www.eclipse.org/jetty/) 或者 [tomcat](http://tomcat.apache.org/)。支持其他的服务，只需要修改下面2个文件即可：`service-broker-snsgw\src\main\webapp\WEB-INF\app.properties`。
<pre>
#service configuration
service.id = ea622002-8e65-4484-88e3-8a0027637647
service.name = snsgw
service.description = Netrix SNS Gateway Instances
service.tags = SNS

#plan info
plan.num=1,2

plan.1.id = 48b5760d-4c5c-49a6-84ca-77d3b1bb8368
plan.1.name = free
plan.1.description = This is a free sns gateway plan. Power by netrix team
		
plan.2.id = ecb66640-ec8c-49e6-9bf8-4db68f24d15a
plan.2.name =  in-charge
plan.2.description = This is a in-charge sns gateway plan. Power by netrix team
						

#nats server
nats = nats://nats:c1oudc0w@10.137.47.205:4222
</pre>
需要修改服务的配置`service configuration` 、 `plan info` 和 `nats server`。 上述配置中的`id`都是guid，直接使用`java`中的 `UUID`就可以生成，如下:
<pre>
UUID uuid = UUID.randomUUID();
System.out.println(uuid.toString());
</pre>

### 自定义Service Node ###

`Service Gateway` 是一个简单的 `java main` 工程，需要自定义的东西比较多，主要的功能是监听 [nats](http://docs.cloudfoundry.org/concepts/architecture/messaging-nats.html) 消息，接收到 `#{service_name}.provision.#{best_node}` 消息之后，生成并且启动服务; 接收到 `#{service_name}.bind.#{best_node}` 绑定服务。

### 添加Service Broker ###

`Service Gateway` 和 `Service Node` 部署启动成功之后，就可以把自定义的`Service Broker`添加到CF中。执行下面的命令即可：
<pre>
#set the target
cf target http://api.10.137.47.221.xip.io

#login
cf login --username admin --password c1oudc0w

#add serivice broker
cf add-service-broker --name snsgw_broker --url http://10.135.189.32:8080/snsgw_broker
 
#list all service brokers
cf service-brokers

#查看所有的service plan
cf curl --mode GET --path /v2/service_plans

#让plan变成public. 注意，`93f8f1c5-7bbf-4d54-94e7-b6ffd762ece4`需要换成上面命令中输出的GUID
cf curl --mode PUT  --path /v2/service_plans/93f8f1c5-7bbf-4d54-94e7-b6ffd762ece4   --body '{"public":true}'

#查看所有的services
cf services --marketplace

#创建服务
cf  create-service

</pre> 
