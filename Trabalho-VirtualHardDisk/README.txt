Desenvolvido como trabalho avaliativo para a disciplina de
Algoritmos e Programação: Estruturas Lineares

 - A classe Teclado é fornecida a todos os alunos de disciplinas de
algoritmos e programação na Unisinos.
 - A classe Driver foi fornecida pelo professor Ernesto Lindstaedt para
a realização deste projeto.

 - Objetivos:
 * Desenvolver um dísco rígido virtual (VHD), ou seja, um programa que
importa arquivos e os armazena no arquivo disco.txt de maneira similar 
ao funcionamento de um disco rígido, utilizando uma File Allocation 
Table (FAT) para organizar os arquivos e diretórios armazenados.
 * Os arquivos devem ser armazenados em clusters de tamanho 
pré-determinado pelo professor, e a FAT deve ser usada para indicar
o endereço do próximo cluster que contenha parte de um arquivo ou diretório.
 * Através do console, deve ser permitido:
	- Navegar pelo VHD, acessando subpastas e pastas pai, ou
indicando um path ("disco/diretorio1/diretorio2", por exemplo);
	- Criar novos diretórios;
	- Importar arquivos para dentro do VHD;
	- Copiar um arquivo ou um diretório (incluindo subpastas)
para outro local no VHD;
	- Mover um arquivo ou um diretório;
	- Remover um arquivo ou diretório do VHD;
	- Visualizar os bytes de um arquivo que esteja armazenado.
	- Visualizar a quantidade de arquivos e diretorios no VHD.

- Inclui arquivos para usar como teste.