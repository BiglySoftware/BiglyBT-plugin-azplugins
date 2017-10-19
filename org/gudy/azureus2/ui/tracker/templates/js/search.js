function f(){document.searching.search.focus();}
	
function search() {
	
		var searchString = document.searching.search.value;
		
		if (searchString == "") {
			f()
		} else {
			window.location.replace(page_url+"?search=" + searchString);
		}
	
}