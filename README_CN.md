## MPXJ 转换器（MPP → CSV）

一个极简的容器化转换器，使用 MPXJ 读取 Microsoft Project 的 MPP 文件，并导出以下 CSV 文件：
- tasks.csv（任务）
- resources.csv（资源）
- assignments.csv（分配）

## 获取/构建镜像

- 方式 A（推荐）：从 Docker Hub 拉取
  - docker pull westrooper/mpxj-convert:latest

- 方式 B：本地构建（多阶段：Maven + JRE）
  - docker build -t local/mpxj-convert:latest .

- 可选：本地构建 fat-jar 并直接运行
  - mvn -q -DskipTests package
  - java -jar target/mpxj-convert.jar /path/to/input.mpp /tmp/out

## 运行（Docker）

将宿主机目录挂载到容器内 /files 后运行：

- docker run --rm -v /absolute/host/folder:/files \
    westrooper/mpxj-convert:latest /files/input.mpp /files/output/myrun [/files/header_zh.properties]

运行完成后会生成：
- /files/output/myrun/tasks.csv
- /files/output/myrun/resources.csv
- /files/output/myrun/assignments.csv

参数说明：
- 第三个参数（表头映射）可选。可提供 properties/CSV/JSON 文件用于本地化表头。
- 表头映射键既支持全限定形式（如 TASK.NAME、RESOURCE.EMAIL_ADDRESS、ASSIGNMENT.FINISH），也支持简写（NAME、EMAIL_ADDRESS、FINISH）。当两者同时存在时，全限定优先。

## 集成 n8n（Execute Command 节点）

情形 1：n8n 能执行 `docker run`（推荐）
- 确保你的 n8n 容器（例如 `n8n_app`）具备：
  - 可用的 Docker CLI（PATH 中有 `docker`，或共享的可执行文件如 `/shared/docker`）
  - 已挂载 /var/run/docker.sock
  - 工作流所需文件已挂载到容器路径 `/files`

在 Execute Command 节点中的示例命令：

- sh -lc '
  in="/files/input/{{$binary.data.fileName}}";
  stem="${in##*/}"; stem="${stem%.*}";
  out="/files/output/${stem}";
  mkdir -p "$out";
  if command -v docker >/dev/null 2>&1; then DOCKER=docker; elif [ -x /shared/docker ]; then DOCKER=/shared/docker; else echo "docker not found"; exit 127; fi;
  "$DOCKER" run --rm --volumes-from n8n_app \
    westrooper/mpxj-convert:latest "$in" "$out" "/files/header_zh.properties";
  echo "DONE -> $out/tasks.csv $out/resources.csv $out/assignments.csv";
  '

情形 2：n8n 当前无法在该节点执行 `docker run`（权限/runner 沙箱限制）
- 方案 A：将该节点切换为在 n8n 主进程执行（关闭 runners），或为 runner 提供与主进程等同的挂载：`/var/run/docker.sock` 与共享的 Docker 可执行文件路径（如 `/shared/docker`）。
- 方案 B：在 docker-compose 中新增一个仅用于“提供卷”的 sidecar（例如 `mpxj_volumes`，命令为 `sleep infinity`）并挂载 `/files`，随后在宿主机或其他有权限的服务中执行 `docker run --volumes-from mpxj_volumes`。

## 说明（Notes）
- CSV 以 UTF-8 且包含 BOM 写出（兼容 Excel），字段适当加引号；日期时间为 ISO-8601（UTC）文本。
- 默认仅导出 MPXJ 检测到“已填充”的字段（任务/资源/分配）。如需自定义选择或格式，可修改 `MppToCsv.java`。
- MPXJ 版本在 `pom.xml` 中指定。

## 鸣谢与许可（Acknowledgements & Licensing）
- 本项目使用 joniles 的 MPXJ 库（https://github.com/joniles/mpxj），其许可为 GNU Lesser General Public License（LGPL）v2.1 或更高版本。MPXJ 仍遵循其原始许可条款。
- 项目许可：MIT（建议）。如你希望与 MPXJ 对齐改为 LGPL‑2.1+，或新增独立 LICENSE 文件，请告知，我会补充到仓库。
- 当分发包含 MPXJ 的二进制/镜像时，请保留对 MPXJ 的 LGPL 许可声明（例如链接到 MPXJ 仓库及其 LICENSE）。

