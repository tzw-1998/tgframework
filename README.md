# tgframework
tgframework is a Spring-Boot-start project to help distributed cluster continuous integration
## How to use it
   ###&nbsp;&nbsp;Configure your Redis server in your application.yml
   ``` 
    spring.redis.host: your host
    spring.redis.password: your password    
   ```
   ###&nbsp;&nbsp; Configure your application type
   ```
    tg.applicationtype: slave
   ```
   Now, run your Spring-Boot project without writing any code  
   If you want to modify the function while it is running, you can start a  master server
   ```
       tg.applicationtype: master
   ```
   ###&nbsp;&nbsp; Architecture diagram
   ![architecture]()