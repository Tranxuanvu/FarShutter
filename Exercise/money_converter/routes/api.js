var express = require('express');
var router = express.Router()
var request = require('request');

router.get('/convert', function(req, res, next) {
  var amount  = req.param('a'),
      from    = req.param('from'),
      to      = req.param('to');

  if(!amount || !from || !to){
    // respon error when miss a param
    res.json({
      error: "Wrong params input required"
    });
  }
  else{
    var url = 'http://rate-exchange.herokuapp.com/fetchRate?from=' + from + '&to=' + to;
    
    // get data responed from url
    request(url, function (error, response, body) {
      if (!error && response.statusCode == 200) {
        var result  = 0,
            rate    = parseFloat(JSON.parse(body).Rate);

        // senario for very small rate(smaller than 0.0000)
        if(rate == 0){
          url = 'http://rate-exchange.herokuapp.com/fetchRate?from=' + to + '&to=' + from;

          request(url, function (error, response, body) {
            if (!error && response.statusCode == 200) {
              rate = 1/parseFloat(JSON.parse(body).Rate);
              result = rate * amount;
              res.json({ amount: amount, from: from, to: to, rate: rate, result: result });
            }
          });
        }
        
        // others
        else{
          result = rate * amount;
          res.json({ amount: amount, from: from, to: to, rate: rate, result: result });
        }
      }
    });
  }
});

module.exports = router;
