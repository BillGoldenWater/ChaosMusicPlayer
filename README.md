### 简介

ChaosMusicPlayer 是一个用于便捷播放音乐的 Minecraft 服务端插件

### 特点:

- 无需客户端安装任何mod
- 可以播放任意音乐
- 多人一起听
- 便捷的自定义设置

### 注:

- 因实现方法受 Minecraft 同时播放声音数(247)的限制 造成播放复杂音乐时噪音较多
- 仅在 1.18.2 paper 测试, 理论上兼容 spigot

### BUG反馈, 建议等:

- 交流群: 263798831
- [GitHub Issues](https://github.com/BiliGoldenWater/ChaosMusicPlayer/issues)

### 下载:

- [GitHub Releases](https://github.com/BiliGoldenWater/ChaosMusicPlayer/releases)

### 使用教程:

1. 下载后放到 任意 文件夹里
2. 使用 jre 运行
3. 运行后找到生成的 `generateResourcePack` 文件夹
4. 进入 `generateResourcePack/output`
5. 其中的 `ChaosMusicPlayer.zip` 就是要在客户端加载的资源包
6. 将插件放到 `服务器目录/plugins` 文件夹里
7. 启动服务器
8. 将音乐文件放到 `服务器目录/plugins/ChaosMusicPlayer/musics` 文件夹里, 仅限wav文件(获取方法见下文)
9. 服务器中输入 /cmp list 即可列出音乐
10. 服务器中输入 /cmp help 可以查看所有命令的详细用法

### 获取wav文件:

1. 安装 ffmpeg
    1. [下载](https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-full.7z)
    2. 解压到一个不常移动的目录
    3. 添加 解压出来的文件夹 里的 bin文件夹 到 环境变量
2. 打开 cmd 或 shell
3. 运行 `ffmpeg -i 输入文件 -c:a pcm_s32be 输出文件.wav`
    - 将输入文件替换成下载的音乐文件的路径
    - 输入文件替换成和输入文件一样的路径再加一个.wav即可
    - 注意如果路径中有空格的话需要在前后添加英文双引号"
    - 例: `ffmpeg -i C:\test.mp3 -c:a pcm_s32be C:\test.mp3.wav`
    - 例2: `ffmpeg -i "C:\te st.mp3" -c:a pcm_s32be "C:\te st.mp3.wav"`
4. 将输出的wav文件放到 `服务器目录/plugins/ChaosMusicPlayer/musics` 即可

### 权限节点:

- `chaosmusicplayer.modify` 拥有的玩家可以修改所有音乐的参数 默认 op 拥有
- `chaosmusicplayer.settings` 拥有的玩家可以查看所有音乐的参数 默认拥有

### 许可协议:

GNU Affero General Public License Version 3
