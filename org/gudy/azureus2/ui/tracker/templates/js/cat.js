function setSize(s, t, h) {

	var sSize = "<font size=2> [" + s + "]<\/font>"
	var ssSize = " [" + s + "]"
	var cat_inner = document.getElementById('c' + t).innerHTML
	var l = sSize.length
	var L = cat_inner.length
	
	if (cat_inner.indexOf(ssSize) >= 0)	{

		if (h == 'Full Listing') {
			document.getElementById('c' + t).innerHTML = cat_inner.substring(0, L-l)
		} else {
			document.getElementById('c' + t).innerHTML = cat_inner
		}
		
	} else {
	
		document.getElementById('c' + t).innerHTML += "<font size=2>" + ssSize + "<\/font>"
	}
}

function unsetSize( s, t, h ) {

	var sSize = "<font size=2> [" + s + "]<\/font>"
	var ssSize = " [" + s + "]"
	var cat_inner = document.getElementById('c' + t).innerHTML
	var l = sSize.length
	var L = cat_inner.length

	if (cat_inner.indexOf(ssSize) >= 0)	{
			document.getElementById('c' + t).innerHTML = cat_inner.substring(0, L-l)
	} else {
			document.getElementById('c' + t).innerHTML = cat_inner
	}

}

function switchdisplay(t, w, h) {

    var tbl  = document.getElementById(t);
    var div = document.getElementById('linkup_' + t);
	var stl;
	
	if (w) {
		if (h == 'Headers Only') {
			stl = 'none'
			setSize( tbl.rows.length-1, t, h );
			document.getElementById("toggleheaders").innerHTML = 'Full Listing'

		} else {
			stl = 'block'
			unsetSize( tbl.rows.length-1, t, h );    			
			document.getElementById("toggleheaders").innerHTML = 'Headers Only'

		}
	} else {
	
		if (tbl.style.display == 'none') {
			stl = 'block';
			unsetSize( tbl.rows.length-1, t, h );
		} else {
			stl = 'none';
			setSize( tbl.rows.length-1, t, h );
		}
	}
	tbl.style.display=stl;
	div.style.display=stl;
	};
	
function displaycatheaders() {

	var header_value = document.getElementById("toggleheaders").innerHTML;
	for(i=1;i<=cat_count;i++) {
		switchdisplay("t"+i, true, header_value )
	}
};

function toggleCatSelection(field) {
	
	if (checkflag == "false") {
		for (i = 0; i < field.length; i++) {
			field[i].checked = true;
		}
		checkflag = "true";
	}
	else {
		for (i = 0; i < field.length; i++) {
		field[i].checked = false; }
		checkflag = "false";
	}
}

var s = "";

function catSelected() {

	var field = document.catchoice.list;
		
		for (i = 0; i < field.length; i++) {
		
				if (field[i].checked == true) {
					
					s += "," + field[i].value;
					
				}
				
		}
		
		s += ","; 
		
		if (s == ",") s = "";

}

function selectCat() {

	if (document.catchoice.all.checked == true) {
		
		window.location.replace(page_url);
	
	} else {

		catSelected();
		
		if (s != "") {
		
			window.location.replace(page_url+"?cat=" + s);
			
		}
		
	}

}

function toggleAll() {

	var field = document.catchoice.list;
	var count = 0;
		
		for (i = 0; i < field.length; i++) {
		
				if (field[i].checked == true) {
					
					count += 1;
					
				}
				
		}
		
		if (count != field.length) {
		
			document.catchoice.all.checked = false;
			checkflag = "false";
		
		} else {
		
			document.catchoice.all.checked = true;
			checkflag = "true";
		
		}

}
