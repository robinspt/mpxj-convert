MPXJ Converter (MPP → CSV)

Minimal, containerized converter that reads Microsoft Project MPP (via MPXJ) and writes CSV files:
- tasks.csv
- resources.csv
- assignments.csv

Build

1) Build the Docker image (multi-stage Maven + JRE):

   docker build -t local/mpxj-convert:1.0.0 .

2) (Optional) Build the fat-jar locally:

   mvn -q -DskipTests package
   java -jar target/mpxj-convert.jar /path/to/input.mpp /tmp/out

Run (Docker)

Example mounting a host folder containing your input/output:

   docker run --rm -v /absolute/host/folder:/files \
     local/mpxj-convert:1.0.0 /files/input.mpp /files/output/myrun

After run, you will find:
- /files/output/myrun/tasks.csv
- /files/output/myrun/resources.csv
- /files/output/myrun/assignments.csv

Integrate with n8n (Execute Command)

If your n8n container is named n8n_app and already mounts /files, you can avoid absolute paths by using --volumes-from:

   sh -lc '
   in="/files/input/{{Date.now()}}-{{$binary.file.fileName}}"; # adapt to actual path in your workflow
   stem="$(basename "${in%.*}")";
   out="/files/output/${stem}";
   mkdir -p "$out";
   docker run --rm --volumes-from n8n_app \
     local/mpxj-convert:1.0.0 "$in" "$out"
   '

Notes
- The converter writes UTF-8 CSV with quoted fields and ISO-8601 UTC datetimes.
- Only a core set of fields are exported to keep the image minimal. You can extend MppToCsv.java as needed.
- MPXJ version is set in pom.xml; bump it if you need newer format support.

