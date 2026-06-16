# 课时统计

个人离线安卓课时统计工具。第一版用于替代纸质记录：添加学生、记录上课、自动计算剩余课时，并支持 CSV 备份导出。

## 已实现功能

- 中文界面
- 本地 SQLite 保存，无登录、无服务器
- 添加和编辑学生
- 添加和编辑学生时可用日期选择器选择购买日期
- 学生页展示总课时、已上课时和剩余课时
- 记录每次上课，固定扣 1 节
- 同一天可记录多节
- 添加上课记录时可选择历史日期并补录多节
- 学生列表手动排序
- 剩余 3 节以内提醒
- 学生详情页查看上课记录
- 学生详情页支持月历查看上课日期
- 删除误记记录后自动恢复课时
- 删除学生，同时删除该学生的上课记录
- 课时用完后“记一节”置灰，不能超过总课时
- 统计页支持今天、本周、本月、本年和自定义日期范围
- 统计页支持月历查看每天上课情况
- 点击统计页中的某一天，可查看当天哪些学生上课以及各上几节
- 导出 CSV 备份

## 构建 APK

本机使用 Java 17 构建：

```powershell
$env:ANDROID_HOME=(Join-Path (Get-Location) 'android-sdk')
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:JAVA_HOME='C:\Users\Lenovo\.cursor\extensions\redhat.java-1.37.0-win32-x64\jre\17.0.13-win32-x86_64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
gradle assembleDebug
```

生成文件：

```text
app/build/outputs/apk/debug/app-debug.apk
```
