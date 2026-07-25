/* Shared theme persistence across all 食记 screens */
(function() {
  var stored = localStorage.getItem('shiji-theme') || 'light';
  document.body.setAttribute('data-theme', stored);

  window.setShijiTheme = function(theme) {
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('shiji-theme', theme);
  };

  window.toggleShijiTheme = function() {
    var next = document.body.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    setShijiTheme(next);
    return next;
  };
})();
