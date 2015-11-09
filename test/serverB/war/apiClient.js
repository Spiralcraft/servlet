/////////////////////////////////////////////////////


///////////////////////////////////////////////////////
//
// API object to call server
//
SPIRALCRAFT.api = (function(self) {

  self.baseURL = "http://localhost:8080/api";

  self.post = (function(method,callback,data) {
    SPIRALCRAFT.ajax.post(self.baseURL+"/"+method,callback,data);
  });
      
  self.get = (function(method,callback) {
    SPIRALCRAFT.ajax.get(self.baseURL+"/"+method,callback);
  });
  
  self.postJSON = (function(method,callback,obj) {
    self.post(method,function(val) { var jval=SPIRALCRAFT.json.parse(val); console.log(jval); callback(jval); },SPIRALCRAFT.json.stringify(obj));
  });

  return self;
}(SPIRALCRAFT.api || {}));


//
// When this JS file is loaded, process the dom to create all the bindings
//
SPIRALCRAFT.dom.registerBodyOnLoad(
    function() { SPIRALCRAFT.webui.processTree(document); return true; }
  );


