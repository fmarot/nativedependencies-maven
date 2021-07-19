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

File liblwjgl = new File( natives, "liblwjgl.so" )
assert assertExistsFile( liblwjgl )

if ( ! System.properties['os.name'].toLowerCase().contains('windows')) {
	File dirWithSymlinks = new File(natives, "dir_with_symlinks")
	File symlinkTarget = new File(dirWithSymlinks , "a.txt" )
	assert assertExistsFile( symlinkTarget )

	File symlink = new File(dirWithSymlinks , "link" )
	assert java.nio.file.Files.isSymbolicLink(symlink.toPath())
	assert new File(symlink.getCanonicalPath()).equals(symlinkTarget);
}

return true
