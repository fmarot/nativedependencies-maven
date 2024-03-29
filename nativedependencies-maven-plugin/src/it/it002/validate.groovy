def assertExistsDirectory( file )
{
  if ( !file.exists() || ! file.isDirectory() )
  {
      println( file.getAbsolutePath() + " file is missing or is not a directory." )
      return false
  }
  return true
}

def assertExistsFile( file )
{
  if ( !file.exists() || file.isDirectory() )
  {
      println( file.getAbsolutePath() + " file is missing or a directory." )
      return false
  }
  return true
}

File target = new File( basedir, "target" )
assert assertExistsDirectory( target )

File natives = new File( target, "natives" )
assert assertExistsDirectory( natives )


if ( System.properties['os.name'].toLowerCase().contains('windows')) {
	File opencv_java_dll = new File( natives, "opencv_java.dll" )
  assert assertExistsFile( opencv_java_dll )
} else if ( System.properties['os.name'].toLowerCase().contains('linux')) {
	File opencv_java_so = new File( natives, "opencv_java.so" )
  assert assertExistsFile( opencv_java_so )
} 


return true
