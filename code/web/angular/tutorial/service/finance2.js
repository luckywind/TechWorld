angular.module('finance2', [])
  .factory('currencyConverter', function() {
    var currencies = ['USD', 'EUR', 'CNY'],
        usdToForeignRates = {
      USD: 1,
      EUR: 0.74,
      CNY: 6.09
    };
    return {
      currencies: currencies,
      convert: convert
    };
 
    function convert(amount, inCurr, outCurr) {
      return amount * usdToForeignRates[outCurr] * 1 / usdToForeignRates[inCurr];
    }
  });