
启动时在控制台输入数据库的链接，用户名和密码。
linux示例：
export DATABASE_URL=jdbc:mysql://localhost:3306/mydb
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=secret
java -jar web-app.jar
windows:
$env:DATABASE_URL = "jdbc:mysql://localhost:3306/test0"
$env:DATABASE_USERNAME = "root"
$env:DATABASE_PASSWORD = ""
