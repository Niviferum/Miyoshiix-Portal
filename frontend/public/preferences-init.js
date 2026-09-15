// Pose le thème et la préférence d'animation sur <html> avant l'affichage.
// Choix mémorisés en priorité, sinon réglages du système.
(function () {
  var root = document.documentElement;
  var theme = null;
  var motion = null;
  try {
    theme = localStorage.getItem('miyoshix-theme');
    motion = localStorage.getItem('miyoshix-animations');
  } catch (e) {}
  var matches = function (query) {
    return !!(window.matchMedia && window.matchMedia(query).matches);
  };
  if (theme !== 'light' && theme !== 'dark') {
    theme = matches('(prefers-color-scheme: dark)') ? 'dark' : 'light';
  }
  if (motion !== 'on' && motion !== 'off') {
    motion = matches('(prefers-reduced-motion: reduce)') ? 'off' : 'on';
  }
  root.setAttribute('data-theme', theme);
  root.setAttribute('data-motion', motion === 'off' ? 'reduced' : 'full');
})();
