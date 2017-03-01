<?php

$files = file("php://input");

$fp = fopen('post.log.txt','a');

fputs($fp,'----- data ------- '.date('Y-m-d H:i:s')."\n");
foreach($files as $file){
	if(is_dir($filee)) continue;
	fputs($fp,"changed: ".$file);
}
fputs($fp,'-----data end-------'."\n");
fclose($fp);

