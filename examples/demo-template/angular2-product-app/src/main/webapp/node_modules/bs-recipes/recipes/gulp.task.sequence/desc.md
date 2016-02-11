
This example highlights a common problem where you don't want to reload
 the browser until a 2 or more slow-running tasks have completed. The solution
 is to create the intermediate task that ensures `browserSync.reload` is not 
 called until both slow tasks are complete.
