$(function() {
	// btn click handler
  $('#btnConvert').on('click', getConvertResult);
});

// generate html result after convert
function getConvertResult(){
	var amount 	= $('#amount').val(),
			from 		= $('#from').val(),
			to 			= $('#to').val();

	var url = '/api/convert?a=' + amount + '&from=' + from + '&to=' + to;

	$.getJSON(url, function(data) {
		var resultContent = '';

		resultContent += '<h2>Result</h2>';
		resultContent += '<span class="number">' + parseFloat(data.amount).format(4, 3) + '</span>';
		resultContent += data.from;
		resultContent += '=';
		resultContent += '<span class="number">' + data.result.format(4, 3) + '</span>';
		resultContent += data.to;

		$('#result').html(resultContent);
	});
}

/**
 * Number.prototype.format(n, x)
 * 
 * @param integer n: length of decimal
 * @param integer x: length of sections
 */
Number.prototype.format = function(n, x) {
  var re = '\\d(?=(\\d{' + (x || 3) + '})+' + (n > 0 ? '\\.' : '$') + ')';
  return this.toFixed(Math.max(0, ~~n)).replace(new RegExp(re, 'g'), '$&,');
};
