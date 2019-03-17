log = function(msg, e) {
    console.log(msg + ":= " + e)
    console.dir(e)
}

phonebook = {
    delete: function() {
        x = new XMLHttpRequest()
        x.onload = function(e) {
            if (e.target.status == 200) {
                window.location.reload();
            }
        }
        x.onerror = function(e) { log("onerror is ", e)}
        x.open("DELETE", window.location.pathname)
        x.send("")
    },
    update: function() {
        x = new XMLHttpRequest()
        x.onload = function(e) {
            if (e.target.status == 204) {
                window.location.reload();
            }
        }
        x.onerror = function(e) { log("onerror is ", e)}
        x.open("PUT", window.location.pathname)
        x.send(new FormData(document.getElementById("entry")))
    }
}
