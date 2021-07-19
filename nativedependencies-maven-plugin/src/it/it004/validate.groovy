import groovy.json.JsonSlurper;

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

int numberOfArtifactsUnpacked = new JsonSlurper().parse(new File(natives, "alreadyUnpackedArtifactsInfo.json")).pathToLastModified.size()
println "numberOfArtifactsUnpacked=${numberOfArtifactsUnpacked}"
// Only the 2 linux artifacts must have been unpacked
assert numberOfArtifactsUnpacked == 2



return true
