## MPXJ Converter (MPP → CSV)

Minimal, containerized converter that reads Microsoft Project MPP (via MPXJ) and writes CSV files:
- tasks.csv
- resources.csv
- assignments.csv

## Build / Get the image

- Option A (recommended): Pull from Docker Hub
```bash
docker pull westrooper/mpxj-convert:latest
```

- Option B: Build locally (multi-stage Maven + JRE)
```bash
docker build -t local/mpxj-convert:latest .
```

- Optional: Build the fat-jar locally
```bash
mvn -q -DskipTests package
java -jar target/mpxj-convert.jar /path/to/input.mpp /tmp/out
```

## Run (Docker)

Example mounting a host folder containing your input/output:

```bash
docker run --rm -v /absolute/host/folder:/files \
  westrooper/mpxj-convert:latest /files/input.mpp /files/output/myrun [/files/header_zh.properties]
```

After run, you will find:
- /files/output/myrun/tasks.csv
- /files/output/myrun/resources.csv
- /files/output/myrun/assignments.csv

Notes on args:
- Third argument (header mapping) is optional. You can provide a properties/CSV/JSON file to localize headers.
- Header keys can be either fully qualified (e.g. TASK.NAME, RESOURCE.EMAIL_ADDRESS, ASSIGNMENT.FINISH) or short form (NAME, EMAIL_ADDRESS, FINISH). Fully qualified keys take precedence.

## Integrate with n8n (Execute Command)

Case 1: n8n can run `docker run` (recommended)
- Ensure your n8n container (e.g. `n8n_app`) has:
  - Docker CLI available (either `docker` in PATH or a shared binary like `/shared/docker`)
  - /var/run/docker.sock mounted
  - Your workflow files mounted at `/files`

Example command in an Execute Command node:

```bash
sh -lc '
in="/files/input/{{$binary.data.fileName}}";
stem="${in##*/}"; stem="${stem%.*}";
out="/files/output/${stem}";
mkdir -p "$out";
chmod 777 "$out";
if command -v docker >/dev/null 2>&1; then DOCKER=docker; elif [ -x /shared/docker ]; then DOCKER=/shared/docker; else echo "docker not found"; exit 127; fi;
"$DOCKER" run --rm --volumes-from n8n_app \
  westrooper/mpxj-convert:latest "$in" "$out" "/files/header_zh.properties";
echo "DONE -> $out/tasks.csv $out/resources.csv $out/assignments.csv";
'
```

Case 2: n8n cannot run `docker run` for this node (permissions/runner sandbox)
- Option A: Execute this node in the n8n main process (disable runners), or grant the runner the same mounts: `/var/run/docker.sock` and the shared Docker binary path (e.g., `/shared/docker`).
- Option B: Add a volumes-only sidecar service in docker-compose (e.g., `mpxj_volumes` with `sleep infinity`) that mounts `/files`. Then invoke `docker run --volumes-from mpxj_volumes` from the host or another privileged service.


## Notes
- CSV is written as UTF-8 with BOM (Excel-friendly), with proper quoting; datetimes are ISO-8601 (UTC) textual values.
- By default we export only populated fields detected by MPXJ for tasks/resources/assignments. You can extend `MppToCsv.java` if you need custom selection or formatting.
- MPXJ version is set in `pom.xml`.

## Acknowledgements & Licensing
- This project uses the MPXJ library by joniles (https://github.com/joniles/mpxj), which is licensed under the GNU Lesser General Public License (LGPL) v2.1 or later. The MPXJ library remains licensed under its original terms.
- Project license: MIT (proposed). If you prefer to license this utility under LGPL-2.1+ to align with MPXJ, or add a dedicated LICENSE file, please confirm and we will add it to the repository.
- When redistributing binaries/images that include MPXJ, ensure the LGPL notice for MPXJ is preserved (e.g., by linking to the MPXJ repository and its LICENSE).
