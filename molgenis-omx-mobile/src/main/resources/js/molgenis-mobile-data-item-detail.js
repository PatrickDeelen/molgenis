var selectedFeatureHref = null;

$(document).bind("mobileinit", function() {
	$(document).on('pageshow', '#feature-page', window.top.molgenis.onDataItemPageShow);
	$(document).on('pagehide', '#feature-page', window.top.molgenis.onDataItemPageHide);
});

(function($, molgenis) {
	"use strict";
	
	var ns = molgenis;
	var restApi = new ns.RestClient();
	
	ns.onDataItemPageShow = function() {
		if (selectedFeatureHref) {
			restApi.getAsync(selectedFeatureHref, null, function(feature) {
				$('.feature-name').html(feature.Name);
				$('#feature-description').html(feature.description);
				$('#feature-datatype').html(feature.dataType);
			
				if (feature.dataType == 'categorical') {
				
					$('#categories').append('<ul data-inset="true" data-role="listview"></ul>');
					$("#categories").trigger("create");
				
					var items = [];
					items.push('<li>Categories</li>');
				
					var q = {q:[{field:'observablefeature_identifier',operator:'EQUALS',value:feature.Identifier}]};
					restApi.getAsync('/api/v1/category', {'q': q}, function(categories) {
						$.each(categories.items, function() {
							items.push('<li class="feature-detail-value">' + this.Name + '</li>');
						});
						$('#categories ul').html(items.join('')).listview('refresh');
					});
				}
			});
		
		} else {
			window.location.href = '/mobile/catalogue';
		}
	}
	
	ns.onDataItemPageHide = function() {
		$('.feature-name').html('&nbsp;');
		$('#feature-description').html('');
		$('#feature-datatype').html('');
		$('#categories').html('');
	}
	
}($, window.top.molgenis = window.top.molgenis || {}));