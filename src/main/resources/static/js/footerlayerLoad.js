function footerlayerLoad(filePath) {
    var overlay = $('<div class="overlay"></div>');
    var content = $('<div class="content"></div>');
    var closeBtn = $('<div class="close-btn">X</div>');
    var iframe = $('<iframe src="' + filePath + '" width="800" height="600"></iframe>');

    closeBtn.on('click', function() {
        overlay.remove();
    });

    content.append(closeBtn);
    content.append(iframe);
    overlay.append(content);
    $('body').append(overlay);
    overlay.fadeIn();
}


