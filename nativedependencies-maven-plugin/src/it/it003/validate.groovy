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


File opencv_java_dll = new File( natives, "opencv_java.dll" )
assert assertExistsFile( opencv_java_dll )

File opencv_java_so = new File( natives, "libopencv_java.so" )
assert assertExistsFile( opencv_java_so )

File opencv_java_jnilib = new File( natives, "libopencv_java.jnilib" )
assert assertExistsFile( opencv_java_jnilib )


return true
