function maybeRemoveExistingTable() {
  var tables = document.getElementsByTagName("table");
  if (tables.length > 0) {
    var table = tables[0];
    document.body.removeChild(table);
  }
}

window._doFetchUrl = function(url, callback) {
  var xhr = new XMLHttpRequest();
  xhr.open("GET", url, true);
  xhr.send();
  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {        
        var response = xhr.responseText;
        var lines = response.split(/\n\r|\n/);
        var table = document.createElement("table");
        table.setAttribute("cellpadding", "0");
        for (var i = 0, n = lines.length; i < n; i++) {
          var row = document.createElement("tr");
          var numberCell = document.createElement("td");
          numberCell.className = "webkit-line-number";
          numberCell.innerText = i + 1;
          var contentCell = document.createElement("td");
          contentCell.className = "webkit-line-content";
          contentCell.innerText = lines[i];
          row.appendChild(numberCell);
          row.appendChild(contentCell);
          table.appendChild(row);
        }
        maybeRemoveExistingTable();
        document.body.appendChild(table);       
      }
      callback(xhr.status);
    }
  }
};