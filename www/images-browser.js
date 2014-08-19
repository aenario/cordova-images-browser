module.exports = window.ImagesBrowser = ImagesBrowser = {

    getImagesList: function (callback) {

        success = function(list) {callback(null, list);}
        error = function(err) {callback(err);}

        return cordova.exec(success, error, "ImagesBrowser", "getImagesList", []);
    },

    getContactsList: function(callback) {
        success = function(list) {callback(null, list);}
        error = function(err) {callback(err);}

        return cordova.exec(success, error, "ImagesBrowser", "getContactsList", []);
    }
};