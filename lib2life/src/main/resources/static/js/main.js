'use strict';

var singleUploadForm = document.querySelector('#singleUploadForm');
var singleFileUploadError = document.querySelector('#singleFileUploadError');
var singleFileUploadSuccess = document.querySelector('#singleFileUploadSuccess');
var editProcessedForm = document.querySelector('#editProcessedForm');
var processedJson = document.querySelector('#processedJson');

var singleFileUploadInput = document.querySelector('#singleFileUploadInput');

var bookTitle = document.querySelector('#bookTitle');
var author = document.querySelector('#author');
var publishingYear = document.querySelector('#publishingYear');
var language = document.querySelector('#language');
var images = document.querySelector('#images');
var pageone = document.querySelector('#pageone');

var search = document.querySelector('#search');
var searchBook =document.querySelector('#searchBook');

/*//display search result in div
function displaySearchResult(results){
	
	var xhr = new XMLHttpRequest();
    xhr.open("POST", "/search");
	
    xhr.onload = function() {
        if(xhr.status == 200) {
        	
        	
        }
    }  
    xhr.send(formData);
}*/

function uploadSingleFile(file) {
    var formData = new FormData();
    formData.append("file", file);
    formData.append("bookTitle", bookTitle.value);
    formData.append("pageone", pageone.value);
    formData.append("images", images.checked);


    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/uploadFile");

    xhr.onload = function() {
        if(xhr.status == 200) {
            singleFileUploadError.style.display = "none";
            singleFileUploadSuccess.style.display = "block";

            var htmlText = jsonToHtml(xhr.responseText);
            var ed = tinyMCE.get('processedJson');
            ed.setContent(htmlText);
        } else {
            singleFileUploadSuccess.style.display = "none";
            singleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
        }
    }

    xhr.send(formData);
}

singleUploadForm.addEventListener('submit', function(event){
    var files = singleFileUploadInput.files;
    if(files.length === 0) {
        singleFileUploadError.innerHTML = "Please select a file";
        singleFileUploadError.style.display = "block";
    }
    uploadSingleFile(files[0]);
    event.preventDefault();
}, true);


function saveJson(json) {
    var formData = new FormData();
    
    // add metadata
    formData.append("bookTitle", bookTitle.value);
    formData.append("author", author.value);
    formData.append("publishingYear", publishingYear.value);
    formData.append("language", language.value);
    
    //add json
    var ed = tinyMCE.get('processedJson');
    var json = htmlToJson(ed.getContent());
    formData.append("json", json);

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/saveJson");

    xhr.onload = function() {
        //console.log(xhr.responseText);
        var response = JSON.parse(xhr.responseText);
        if(xhr.status == 200) {
            singleFileUploadError.style.display = "none";
            processedJson.innerHTML = xhr.responseText;
            singleFileUploadSuccess.style.display = "block";
            singleFileUploadSuccess.innerHTML = "Json saved!";
        } else {
            singleFileUploadSuccess.style.display = "none";
            singleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
        }
    }

    xhr.send(formData);
}

editProcessedForm.addEventListener('submit', function(event){
    event.preventDefault();
    saveJson();
}, true);


function jsonToHtml(jsonText) {
	var jsonContent = JSON.parse(jsonText);
	
	var htmlText = "";
	
	for(var c=0; c<jsonContent.length; c++) {
		var chapterContent = jsonContent[c];
		
		if(chapterContent.chapterTitle) {
			htmlText += "<h4>" + chapterContent.chapterTitle + "</h4>";
		}
		
		if(chapterContent.paragraphs) {
			if(typeof(chapterContent.paragraphs) === "string") {
				htmlText += "<p>" + chapterContent.paragraphs + "</p>";
			} else if(typeof(chapterContent.paragraphs) === "object" && chapterContent.paragraphs.length) {
				for(var p=0; p<chapterContent.paragraphs.length; p++) {
					var paragraph = chapterContent.paragraphs[p];
					
					htmlText += "<p>" + paragraph + "</p>";
				}
			} 
		}
	}
	
	return htmlText;
}

function htmlToJson(htmlText) {
	var node = new tinymce.html.DomParser().parse(htmlText);

	var jsonContent = [];
	var currentChapter = null;
	
	//go from each node to the next
	while(node = node.walk()) {
		//we only care about the text nodes and their parent
		if(node.name === "#text") {
			switch(node.parent.name) {
				case "h4":
					currentChapter = {};
					jsonContent.push(currentChapter);
					currentChapter.chapterTitle = node.value;
					break;
				case "p":
					//check if current chapter is defined, in case the first paragraph doesn't have a header above it
					if(!currentChapter) {
						currentChapter = {};
						jsonContent.push(currentChapter);
					}
					
					if(!currentChapter.paragraphs) {
						currentChapter.paragraphs = [];
					}
					currentChapter.paragraphs.push(node.value);
			}
		}
	}
	
	return JSON.stringify(jsonContent);
}
