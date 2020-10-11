# m-mall-order （分布式微服务订单模块）

### 项目介绍
#### 开发工具
> * IntelliJ IDEA
> * [破解教程](https://blog.csdn.net/yy891136/article/details/90266539)
#### 环境配置
> * [Jdk1.8及以上版本](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
> * [Maven3.5及以上版本](http://maven.apache.org/download.cgi)

 maven setting.xml 配置 
> 1 添加servers授权信息
````xml
    <server>
      <id>meifute-releases</id>
      <username>admin</username>
      <password>Meifute@12asw1</password>
    </server>
    <server>
      <id>meifute-snapshots</id>
      <username>admin</username>
      <password>Meifute@12asw1</password>
    </server>
	<server>
      <id>public</id>
      <username>admin</username>
      <password>Meifute@12asw1</password>
    </server>
````
> 2 添加maven镜像仓库地址
````xml
    <mirror>
      <id>nexus-mirror</id>
      <mirrorOf>*</mirrorOf>
      <url>http://maven.meifute.com/repository/maven-public/</url>
    </mirror>
````
#### 技术框架
> * SpringBoot 1.5.9
> * SpringCloud Dalston.RELEASE 
> * LCN 4.1.0
> * Mybatis-Plus 2.1
> * Druid 1.0.29
> * OAuth2 2.0
> * Logback 4.9
> * Liquibase 3.5.1

#### 目录结构
````
端口：8024
| -- order-api     #内部服务调用暴露api接口
| -- order-web     #项目主体web结构
    | -- com.codingapi.tx.springclod.feign #自定义feign拦截器
    | -- com.meifute.core
        | -- component   
        | -- config      
        | -- controller  
        | -- feignclient 
        | -- mapper      
        | -- model       
        | -- service     
        | -- uitl        
        | -- MallOrderApplication.java 
    | -- resources
        | -- liquibase   #数据库版本控制
        | -- application.yml
        | -- application-client.yml
        | -- application-local.yml
        | -- application-prod.yml
        | -- application-prod-b.yml
        | -- logback-spring.xml
        | -- tx.properties
````
#### 接口声明
Controller类暴露restful接口，使用sawgger2注解@Api(tags = "orderCenter", description = "订单中心")，
方法使用@ApiOperation注解生成接口文档，例如：
````java
@ApiOperation(value = "获取运费", notes = "获取运费")
@PostMapping(value = "/order/freight")
public ResponseEntity<MallResponse<BigDecimal>> getPostFee(@RequestBody GetPostFeeParam getPostFeeParam) {
    BigDecimal postFee = orderInfoService.getPostFee(getPostFeeParam);
    return ResponseEntity.ok(successResult(postFee));
}
````
内部微服务之间feign通信使用OrderApiService，例如：
````java
@ApiOperation(value = "根据orderId查询订单信息", notes = "根据orderId查询订单信息")
@GetMapping("/get/order/by/id")
MallOrderInfo getMallOrderInfoById(@RequestParam("orderId") String orderId);
````
指定配置文件启动
````xml
1. 修改application.xml 中的active.profiles 为local或client
2. 运行MallOrderAppcation.java启动类
````
本地环境打包
````
mvn clean install
````
#### 订单模块包含以下接口：
> * 用户下单（积分下单、产品下单）
> * 订单关联商品信息
> * 敏感订单审核、下级订单审核
> * 后台订单列表相关操作接口
> * 订单反馈
> * 自动推单、京东物流对接
> * 订单报表统计
> * 历史运费订单补调
> * 等等




