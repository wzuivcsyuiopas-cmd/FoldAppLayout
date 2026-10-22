"""Check the compiled artifact's platform/API and permission boundaries."""
import re
import subprocess
import sys
import zipfile
from pathlib import Path

apk = Path('build/FoldAppLayout.apk')
badging = subprocess.check_output([sys.argv[1], 'dump', 'badging', str(apk)], text=True)
assert "minSdkVersion:'34'" in badging, 'Broadcast identity needs Android 14+'
assert "targetSdkVersion:'34'" in badging
permissions = re.findall(r"uses-permission: name='([^']+)'", badging)
assert permissions == ['android.permission.QUERY_ALL_PACKAGES'], permissions
with zipfile.ZipFile(apk) as archive:
    assert archive.read('assets/xposed_init').decode().strip() == 'local.codex.applayout.Hook'
    assert 'classes.dex' in archive.namelist()
    assert not any(name.endswith(('.jks', '.keystore', '.java', '.jar')) for name in archive.namelist())
print('PASS: compiled min/target SDK, permissions, Xposed entry point, artifact contents')
