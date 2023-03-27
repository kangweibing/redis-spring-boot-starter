# redis快速使用

### 极少的依赖及配置信息，实现快速使用redis的使用
配置明细
```yaml
spring:
  #redis集群配置
  redis:
    timeout: 6000ms
    database: 0
    cluster:
      nodes:
        - 127.0.0.1:6379
        - 127.0.0.2:6379
        - 127.0.0.3:6379
    password: 123456
    lettuce:
      cluster:
        refresh:
          period: 10000
          adaptive: true
      pool:
        #最大空闲连接
        max-idle: 100
        #最小空闲连接
        min-idle: 1
        #最大连接数(负数表示没有限制)
        max-active: 1000
        #最大阻塞等待时间(负数表示没有限制)
        max-wait: -1
        #连接超时时间(毫秒)
        timeout: 0
        
spring        
  #redis单机配置
  redis:
  #redis服务器地址
  host: localhost
  #redis服务器连接端口
  port: 6379
  lettuce:
    pool:
      #最大空闲连接
      max-idle: 100
      #最小空闲连接
      min-idle: 1
      #最大连接数(负数表示没有限制)
      max-active: 1000
      #最大阻塞等待时间(负数表示没有限制)
      max-wait: -1