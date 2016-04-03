Instructions:

1. BackupService
Start a peer using the following command
        java BackupService <PEER ID> <MC CHANNEL ADDR> <MC PORT> <MDB CHANNEL ADDR> <MDB CHANNEL PORT> <MDR CHANNEL ADDR> <MDR CHANNEL PORT>
        eg: BackupService 01 224.0.0.13 1111 224.0.0.13 1112 224.0.0.13 1113

The first peer to initiate will create the RMI registry, the following peers will connect to it.

2. TestApp

2.1 Backup
        java TestApp <PEER ID> BACKUP <FILE> <REP DEGREE>
        eg: TestApp 01 BACKUP image.jpg 2

2.2 Backup Enhanced
        java TestApp <PEER ID> BACKUPENH <FILE> <REP DEGREE>
        eg: TestApp 01 BACKUPENH image.jpg 2

2.3 Restore
        java TestApp <PEER ID> RESTORE <FILE>
        eg: TestApp 01 RESTORE image.jpg

2.4 Restore Enhanced
        java TestApp <PEER ID> RESTOREENH <FILE>
        eg: TestApp 01 RESTOREENH image.jpg

2.5 Reclaim
        java TestApp <PEER ID> RECLAIM <BYTES TO RECLAIM>
        eg: TestApp 01 RECLAIM 100

2.6 Delete
        java TestApp <PEER ID> DELETE <FILE>
        eg: TestApp 01 DELETE image.jpg