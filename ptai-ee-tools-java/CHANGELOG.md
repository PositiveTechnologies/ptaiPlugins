## v.3.6.1
### 20201209
+ [Feature] CLI, Jenkins and Teamcity plugins are refactored and use same AstJob approach
+ [Feature] Jenkins and CLI plugins reporting settings changed to allow generation of raw issues JSON and filtered PDF/HTML reports, XML/JSON data exports
+ [Feature] All the HTTP requests and responses are logged using FINEST level
+ [Feature] CLI, Jenkins and Teamcity plugins are now support insecure SSL connections i.e. without CA certificate chain verification
+ [Feature] Valid AST result statuses are SUCCESS, FAILED and INTERRUPTED. If AST is done then initial result is SUCCESS. If policy defined and fail-if-failed is on, status changes to FAILED, if no policy or assessment succeeds and there were scan errors or warnings and fail-if-unstable is on, then status also changes to FAILED    
+ [Fix] Due to some limitations of HTTP/2 API (sometimes it answers HTTP 401 for valid authentication token) all REST API clients now use HTTP/1 protocol
### 20201209
+ [Fix] Restored functions of Teamcity plugin, plan to implement async scan and report generation
 